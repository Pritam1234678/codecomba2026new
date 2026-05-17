import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

/**
 * Test cases are no longer managed separately.
 * They are embedded inside the Solution Harness (solutionTemplate) of each
 * CodeSnippet. Redirect admins to the problem edit page where the harness is edited.
 */
export default function ManageTestCases() {
  const { id } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    navigate(`/admin/problems/${id}/edit`, { replace: true });
  }, [id, navigate]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-gray-400 text-lg">Redirecting to problem editor...</div>
    </div>
  );
}
