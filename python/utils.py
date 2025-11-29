#!/usr/bin/env python3
"""
공통 유틸리티 함수
텍스트 처리, 검증 등의 공통 기능을 제공합니다.
"""

import re
from typing import List, Optional


class TextUtils:
    """텍스트 처리 유틸리티"""
    
    # 컴파일된 정규표현식 (성능 최적화)
    HTML_TAG_PATTERN = re.compile(r'<[^>]+>')
    SPECIAL_CHAR_PATTERN = re.compile(r'[^\w\s가-힣]')
    MULTI_SPACE_PATTERN = re.compile(r'\s+')
    
    # 한국어 불용어
    KOREAN_STOPWORDS = {
        '이', '가', '을', '를', '의', '에', '에서', '와', '과', '도', '로', '으로',
        '은', '는', '이다', '있다', '하다', '되다', '그', '것', '수', '등', '및',
        '또한', '또', '그리고', '하지만', '그러나', '그런데', '그래서', '따라서'
    }
    
    @staticmethod
    def clean_html(text: str) -> str:
        """HTML 태그 제거"""
        if not text:
            return ""
        return TextUtils.HTML_TAG_PATTERN.sub('', text)
    
    @staticmethod
    def remove_special_chars(text: str) -> str:
        """특수문자 제거 (한글, 영문, 숫자만 유지)"""
        if not text:
            return ""
        return TextUtils.SPECIAL_CHAR_PATTERN.sub(' ', text)
    
    @staticmethod
    def normalize_text(text: str) -> str:
        """텍스트 정규화 (HTML 제거 + 특수문자 제거 + 공백 정리)"""
        if not text:
            return ""
        text = TextUtils.clean_html(text)
        text = TextUtils.remove_special_chars(text)
        text = TextUtils.MULTI_SPACE_PATTERN.sub(' ', text).strip()
        return text
    
    @staticmethod
    def extract_keywords(text: str, min_length: int = 2) -> List[str]:
        """키워드 추출 (불용어 제거)"""
        if not text:
            return []
        
        normalized = TextUtils.normalize_text(text)
        words = normalized.split()
        
        keywords = [
            w.strip() for w in words 
            if len(w.strip()) >= min_length 
            and w.strip() not in TextUtils.KOREAN_STOPWORDS
        ]
        
        return keywords
    
    @staticmethod
    def extract_sentences(text: str) -> List[str]:
        """문장 추출"""
        if not text:
            return []
        
        # 문장 구분자로 분리
        sentences = re.split(r'[.!?。！？]\s+|[\n\r]+', text)
        
        # 빈 문장 제거 및 최소 길이 필터링
        sentences = [
            s.strip() for s in sentences 
            if s.strip() and len(s.strip()) > 5
        ]
        
        return sentences


class ValidationUtils:
    """입력 검증 유틸리티"""
    
    @staticmethod
    def validate_user_id(user_id: int) -> bool:
        """사용자 ID 검증"""
        if not isinstance(user_id, int):
            return False
        if user_id <= 0:
            return False
        if user_id > 2**31 - 1:  # INT 최대값
            return False
        return True
    
    @staticmethod
    def validate_text_input(text: str, max_length: int = 10000) -> bool:
        """텍스트 입력 검증"""
        if not isinstance(text, str):
            return False
        if len(text) > max_length:
            return False
        # 악성 패턴 검사
        malicious_patterns = [
            r'<script',
            r'javascript:',
            r'onerror=',
            r'onclick=',
            r'eval\(',
            r'exec\('
        ]
        for pattern in malicious_patterns:
            if re.search(pattern, text, re.IGNORECASE):
                return False
        return True
    
    @staticmethod
    def validate_limit(limit: int, min_limit: int = 1, max_limit: int = 100) -> bool:
        """제한값 검증"""
        if not isinstance(limit, int):
            return False
        return min_limit <= limit <= max_limit


