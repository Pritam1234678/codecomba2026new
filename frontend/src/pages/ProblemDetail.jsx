import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import api from '../services/api';

const ProblemDetail = () => {
  const { id } = useParams(); // problemId or contestId? Route is /contests/:id, which implies Contest Detail. 
  // Let's assume we show a list of problems for a contest first.
  // BUT simplified flow: /contests/:id -> List of Problems. 
  // Then /problems/:id -> Solve.
  // I'll implement ContestDetail here which lists problems.
  // Wait, I named the file ProblemDetail.jsx. I should split work.
  
  return <div>Problem Detail Stub</div>;
};

export default ProblemDetail;
