#!/usr/bin/env python3
"""
AI 기반 게시글 요약 시스템
게시글 본문을 분석하여 간결한 요약을 생성합니다.
"""

import json
import sys
import re
from typing import Dict, List


class AISummarizer:
    """AI 요약기"""
    
    def __init__(self):
        self.max_length = 200  # 최대 요약 길이
    
    def clean_text(self, text: str) -> str:
        """텍스트 정리"""
        if not text:
            return ""
        
        # HTML 태그 제거
        text = re.sub(r'<[^>]+>', '', text)
        
        # 연속된 공백 제거
        text = re.sub(r'\s+', ' ', text)
        
        return text.strip()
    
    def extract_sentences(self, text: str) -> List[str]:
        """문장 추출"""
        # 문장 구분자로 분리
        sentences = re.split(r'[.!?。！？]\s+', text)
        
        # 빈 문장 제거
        sentences = [s.strip() for s in sentences if s.strip()]
        
        return sentences
    
    def calculate_sentence_score(self, sentence: str, keywords: List[str]) -> float:
        """문장 중요도 점수 계산"""
        if not sentence:
            return 0.0
        
        score = 0.0
        sentence_lower = sentence.lower()
        
        # 키워드 포함 여부
        for keyword in keywords:
            if keyword.lower() in sentence_lower:
                score += 2.0
        
        # 문장 길이 (너무 짧거나 길면 감점)
        length = len(sentence)
        if 20 <= length <= 100:
            score += 1.0
        elif length < 10 or length > 150:
            score -= 0.5
        
        # 위치 가중치 (앞부분 문장에 가중치)
        # 이 부분은 전체 텍스트에서의 위치를 알아야 하므로 별도 처리 필요
        
        return score
    
    def summarize(self, content: str, max_length: int = 200) -> Dict:
        """본문 요약"""
        if not content:
            return {
                'summary': '',
                'error': '본문이 비어있습니다.'
            }
        
        # 텍스트 정리
        cleaned_text = self.clean_text(content)
        
        if len(cleaned_text) <= max_length:
            return {
                'summary': cleaned_text,
                'original_length': len(cleaned_text),
                'summary_length': len(cleaned_text)
            }
        
        # 문장 추출
        sentences = self.extract_sentences(cleaned_text)
        
        if not sentences:
            return {
                'summary': cleaned_text[:max_length] + '...',
                'original_length': len(cleaned_text),
                'summary_length': max_length
            }
        
        # 키워드 추출 (빈도 기반)
        words = re.findall(r'\b\w+\b', cleaned_text.lower())
        word_freq = {}
        for word in words:
            if len(word) >= 2:  # 2글자 이상만
                word_freq[word] = word_freq.get(word, 0) + 1
        
        # 상위 키워드 선택
        top_keywords = sorted(word_freq.items(), key=lambda x: x[1], reverse=True)[:10]
        keywords = [word for word, _ in top_keywords]
        
        # 문장 점수 계산
        sentence_scores = []
        for i, sentence in enumerate(sentences):
            score = self.calculate_sentence_score(sentence, keywords)
            # 앞부분 문장에 가중치
            position_weight = 1.0 - (i / len(sentences)) * 0.3
            score *= position_weight
            sentence_scores.append((sentence, score, i))
        
        # 점수 순으로 정렬
        sentence_scores.sort(key=lambda x: x[1], reverse=True)
        
        # 요약 생성
        summary_sentences = []
        current_length = 0
        
        for sentence, score, original_index in sentence_scores:
            if current_length + len(sentence) <= max_length:
                summary_sentences.append((original_index, sentence))
                current_length += len(sentence) + 1  # +1 for space
            else:
                break
        
        # 원래 순서대로 정렬
        summary_sentences.sort(key=lambda x: x[0])
        summary = ' '.join([s for _, s in summary_sentences])
        
        # 최대 길이 초과 시 자르기
        if len(summary) > max_length:
            summary = summary[:max_length-3] + '...'
        
        return {
            'summary': summary,
            'original_length': len(cleaned_text),
            'summary_length': len(summary),
            'sentence_count': len(summary_sentences),
            'total_sentences': len(sentences)
        }


def main():
    """메인 함수"""
    if len(sys.argv) < 2:
        print(json.dumps({
            'error': '본문이 필요합니다.',
            'usage': 'python ai_summary.py "<content>" [max_length]'
        }), file=sys.stderr)
        sys.exit(1)
    
    content = sys.argv[1]
    max_length = int(sys.argv[2]) if len(sys.argv) > 2 else 200
    
    try:
        summarizer = AISummarizer()
        result = summarizer.summarize(content, max_length)
        
        print(json.dumps(result, ensure_ascii=False, indent=2))
        
    except Exception as e:
        print(json.dumps({
            'error': str(e)
        }), file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()

