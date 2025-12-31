import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { gsap } from 'gsap';
import axios from 'axios';
import ProblemService from '../services/problem.service';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

export default function EditProblem() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [contestId, setContestId] = useState(null);

  const headerRef = useRef(null);
  const formRef = useRef(null);

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    inputFormat: '',
    outputFormat: '',
    constraints: '',
    timeLimit: 1000,
    memoryLimit: 256,
    example1: '',
    example2: '',
    example3: '',
    images: '',
    active: true
  });

  const [snippets, setSnippets] = useState({
    JAVA: { starterCode: '', solutionTemplate: '' },
    CPP: { starterCode: '', solutionTemplate: '' },
    PYTHON: { starterCode: '', solutionTemplate: '' },
    JAVASCRIPT: { starterCode: '', solutionTemplate: '' },
    C: { starterCode: '', solutionTemplate: '' }
  });

  const [activeSnippetTab, setActiveSnippetTab] = useState('JAVA');

  useEffect(() => {
    loadProblem();
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

  const loadProblem = async () => {
    try {
      const token = localStorage.getItem('token');
      const headers = { Authorization: `Bearer ${token}` };

      const response = await axios.get(`${API_URL}/problems/${id}`, { headers });
      const problem = response.data;

      setFormData({
        title: problem.title || '',
        description: problem.description || '',
        inputFormat: problem.inputFormat || '',
        outputFormat: problem.outputFormat || '',
        constraints: problem.constraints || '',
        timeLimit: problem.timeLimit || 1000,
        memoryLimit: problem.memoryLimit || 256,
        example1: problem.example1 || '',
        example2: problem.example2 || '',
        example3: problem.example3 || '',
        images: problem.images || '',
        active: problem.active !== undefined ? problem.active : true
      });

      if (problem.contestId) {
        setContestId(problem.contestId);
      }

      // Fetch code snippets
      try {
        const snippetsRes = await ProblemService.getSnippets(id);
        const snippetMap = {
          JAVA: { starterCode: '', solutionTemplate: '' },
          CPP: { starterCode: '', solutionTemplate: '' },
          PYTHON: { starterCode: '', solutionTemplate: '' },
          JAVASCRIPT: { starterCode: '', solutionTemplate: '' },
          C: { starterCode: '', solutionTemplate: '' }
        };

        snippetsRes.data.forEach(snippet => {
          snippetMap[snippet.language] = {
            starterCode: snippet.starterCode,
            solutionTemplate: snippet.solutionTemplate
          };
        });

        setSnippets(snippetMap);
      } catch (err) {
        console.log('No snippets found or error loading snippets');
      }

      setLoading(false);
    } catch (err) {
      setError('Failed to load problem');
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : (type === 'number' ? parseInt(value) : value)
    }));
  };

  const handleSnippetChange = (language, field, value) => {
    setSnippets(prev => ({
      ...prev,
      [language]: {
        ...prev[language],
        [field]: value
      }
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');

    try {
      const token = localStorage.getItem('token');

      // Convert empty strings to null for optional fields
      const problemData = {
        ...formData,
        example1: formData.example1?.trim() || null,
        example2: formData.example2?.trim() || null,
        example3: formData.example3?.trim() || null,
        images: formData.images?.trim() || null
      };

      await axios.put(`${API_URL}/admin/problems/${id}`, problemData, {
        headers: { Authorization: `Bearer ${token}` }
      });

      // Save snippets
      const snippetsArray = Object.entries(snippets).map(([lang, data]) => ({
        language: lang,
        starterCode: data.starterCode,
        solutionTemplate: data.solutionTemplate
      }));

      await ProblemService.saveAllSnippets(id, snippetsArray);

      if (contestId) {
        navigate(`/admin/contests/${contestId}/problems`);
      } else {
        navigate('/admin/contests');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update problem');
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
            <h1 className="text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">Edit Problem</h1>
            <p className="text-sm text-gray-500">Update problem details</p>
          </div>
          <button
            onClick={() => contestId ? navigate(`/admin/contests/${contestId}/problems`) : navigate('/admin/contests')}
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
          <label className="block text-sm font-medium text-gray-300 mb-2">Title *</label>
          <input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleChange}
            required
            className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white"
            placeholder="e.g., Two Sum"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Description *</label>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleChange}
            required
            rows={6}
            className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white resize-none"
            placeholder="Problem description..."
          />
        </div>

        <div className="grid grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Input Format</label>
            <textarea
              name="inputFormat"
              value={formData.inputFormat}
              onChange={handleChange}
              rows={3}
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Output Format</label>
            <textarea
              name="outputFormat"
              value={formData.outputFormat}
              onChange={handleChange}
              rows={3}
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white resize-none"
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-2">Constraints</label>
          <textarea
            name="constraints"
            value={formData.constraints}
            onChange={handleChange}
            rows={3}
            className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white resize-none"
            placeholder="e.g., 1 <= n <= 10^5"
          />
        </div>

        <div className="grid grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Time Limit (ms)</label>
            <input
              type="number"
              name="timeLimit"
              value={formData.timeLimit}
              onChange={handleChange}
              min="100"
              step="100"
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">Memory Limit (MB)</label>
            <input
              type="number"
              name="memoryLimit"
              value={formData.memoryLimit}
              onChange={handleChange}
              min="64"
              step="64"
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white"
            />
          </div>
        </div>

        {/* Optional Examples Section */}
        <div className="border-t border-[#3a3a3a] pt-6 mt-6">
          <h3 className="text-lg font-semibold text-white mb-4">Optional Examples</h3>

          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Example 1 (Optional)
            </label>
            <textarea
              name="example1"
              value={formData.example1}
              onChange={handleChange}
              rows={4}
              placeholder="Input: nums = [3,2,1,4,5], k = 4&#10;Output: 3&#10;Explanation: The subarrays that have a median equal to 4 are: [4], [4,5] and [1,4,5]."
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white resize-none font-mono text-sm"
            />
          </div>

          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Example 2 (Optional)
            </label>
            <textarea
              name="example2"
              value={formData.example2}
              onChange={handleChange}
              rows={4}
              placeholder="Input: nums = [2,3,1], k = 3&#10;Output: 1&#10;Explanation: [3] is the only subarray that has a median equal to 3."
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white resize-none font-mono text-sm"
            />
          </div>

          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Example 3 (Optional)
            </label>
            <textarea
              name="example3"
              value={formData.example3}
              onChange={handleChange}
              rows={4}
              placeholder="Input: nums = [1,2,3], k = 2&#10;Output: 0&#10;Explanation: There are no subarrays with median equal to 2."
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white resize-none font-mono text-sm"
            />
          </div>

          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Images (Optional)
              <span className="text-xs text-gray-500 ml-2">Comma-separated URLs</span>
            </label>
            <input
              type="text"
              name="images"
              value={formData.images}
              onChange={handleChange}
              placeholder="https://example.com/image1.png, https://example.com/image2.png"
              className="w-full px-4 py-3 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg focus:ring-2 focus:ring-green-500/50 focus:border-green-500/50 text-white"
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

        {/* Code Snippets Section */}
        <div className="mt-6 border-t border-[#3a3a3a] pt-6">
          <h3 className="text-lg font-semibold text-white mb-4">Code Snippets</h3>

          {/* Language Tabs */}
          <div className="flex gap-2 mb-4 overflow-x-auto">
            {['JAVA', 'CPP', 'PYTHON', 'JAVASCRIPT', 'C'].map(lang => (
              <button
                key={lang}
                type="button"
                onClick={() => setActiveSnippetTab(lang)}
                className={`px-4 py-2 rounded-lg font-medium transition-colors ${activeSnippetTab === lang
                  ? 'bg-green-500 text-white'
                  : 'bg-[#2a2a2a] text-gray-400 hover:bg-[#3a3a3a]'
                  }`}
              >
                {lang === 'CPP' ? 'C++' : lang === 'JAVASCRIPT' ? 'JavaScript' : lang}
              </button>
            ))}
          </div>

          {/* Snippet Fields */}
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Starter Code (shown to user)
              </label>
              <textarea
                value={snippets[activeSnippetTab].starterCode}
                onChange={(e) => handleSnippetChange(activeSnippetTab, 'starterCode', e.target.value)}
                rows={8}
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white font-mono text-sm focus:ring-2 focus:ring-green-500/50 resize-none"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Solution Template (for execution)
              </label>
              <textarea
                value={snippets[activeSnippetTab].solutionTemplate}
                onChange={(e) => handleSnippetChange(activeSnippetTab, 'solutionTemplate', e.target.value)}
                rows={12}
                className="w-full px-4 py-2 bg-[#2a2a2a] border border-[#3a3a3a] rounded-lg text-white font-mono text-sm focus:ring-2 focus:ring-green-500/50 resize-none"
              />
              <p className="text-xs text-gray-500 mt-1">Use USER_CODE_PLACEHOLDER where user code will be inserted</p>
            </div>
          </div>
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
            onClick={() => navigate(`/admin/problems/${id}/testcases`)}
            className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-blue-400 rounded-lg transition-all"
          >
            Manage Test Cases →
          </button>
          <button
            type="button"
            onClick={() => contestId ? navigate(`/admin/contests/${contestId}/problems`) : navigate('/admin/contests')}
            className="px-6 py-3 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 hover:text-red-400 rounded-lg transition-all"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
