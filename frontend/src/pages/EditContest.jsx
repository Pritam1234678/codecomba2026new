import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { gsap } from 'gsap';
import contestService from '../services/contest.service';

export default function EditContest() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const headerRef = useRef(null);
  const formRef = useRef(null);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    startTime: '',
    endTime: '',
    active: false
  });

  useEffect(() => {
    loadContest();
  }, [id]);

  useEffect(() => {
    if (!loading && headerRef.current && formRef.current) {
      const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });

      tl.from(headerRef.current, {
        opacity: 0,
        y: 30,
        duration: 0.8
      })
        .from(formRef.current, {
          opacity: 0,
          y: 20,
          duration: 0.6
        }, '-=0.3');
    }
  }, [loading]);

  const loadContest = async () => {
    try {
      const response = await contestService.getContestById(id);
      const contest = response.data;

      const formatDateTime = (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return date.toISOString().slice(0, 16);
      };

      setFormData({
        name: contest.name || '',
        description: contest.description || '',
        startTime: formatDateTime(contest.startTime),
        endTime: formatDateTime(contest.endTime),
        active: contest.active || false
      });
      setLoading(false);
    } catch (err) {
      setError('Failed to load contest');
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');

    try {
      await contestService.updateContest(id, formData);
      navigate('/admin/contests');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update contest');
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400 text-lg">Loading...</div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 space-y-6">
      <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-8 shadow-2xl">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">Edit Contest</h1>
            <p className="text-sm text-gray-500">Update contest details</p>
          </div>
          <button
            onClick={() => navigate('/admin/contests')}
            className="px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 rounded-lg transition-all text-gray-300"
          >
            ← Back
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 text-red-400 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      <form ref={formRef} onSubmit={handleSubmit} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-8 space-y-6 shadow-xl">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Contest Name *</label>
          <input
            type="text"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
            className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Description *</label>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleChange}
            required
            rows={4}
            className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white resize-none"
          />
        </div>

        <div className="grid grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Start Time *</label>
            <input
              type="datetime-local"
              name="startTime"
              value={formData.startTime}
              onChange={handleChange}
              required
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white [color-scheme:dark]"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">End Time *</label>
            <input
              type="datetime-local"
              name="endTime"
              value={formData.endTime}
              onChange={handleChange}
              required
              min={formData.startTime}
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white [color-scheme:dark]"
            />
          </div>
        </div>

        <div className="flex items-center">
          <input
            type="checkbox"
            name="active"
            checked={formData.active}
            onChange={handleChange}
            className="w-5 h-5 text-green-500 bg-[#2a2a2a] border-[#3a3a3a] rounded focus:ring-green-500/50"
          />
          <label className="ml-3 text-sm font-medium text-gray-300">Active (visible to users)</label>
        </div>

        <div className="flex gap-4 pt-4 border-t border-white/10">
          <button
            type="submit"
            disabled={saving}
            className="flex-1 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold py-3 px-6 rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105 disabled:opacity-50 disabled:hover:scale-100"
          >
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
          <button
            type="button"
            onClick={() => navigate(`/admin/contests/${id}/problems`)}
            className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-green-400 rounded-lg transition-all"
          >
            Manage Problems →
          </button>
          <button
            type="button"
            onClick={() => navigate('/admin/contests')}
            className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-red-400 rounded-lg transition-all"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
