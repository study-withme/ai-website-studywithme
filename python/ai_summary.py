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
    
    def extract_structured_info(self, content: str) -> Dict[str, str]:
        """구조화된 정보 추출"""
        info = {
            'target_users': '',
            'level': '',
            'process': '',
            'summary': ''
        }
        
        # 섹션별 키워드 패턴
        target_user_patterns = [
            r'이런\s*분이면\s*좋아요[:\s]*([^진행방식이런]*?)(?=진행\s*방식|이런|$)',
            r'원하는\s*사람[:\s]*([^진행방식이런]*?)(?=진행\s*방식|이런|$)',
            r'참여\s*대상[:\s]*([^진행방식이런]*?)(?=진행\s*방식|이런|$)',
            r'모집\s*대상[:\s]*([^진행방식이런]*?)(?=진행\s*방식|이런|$)',
        ]
        
        level_patterns = [
            r'레벨[:\s]*([^진행방식이런]*?)(?=진행\s*방식|이런|$)',
            r'수준[:\s]*([^진행방식이런]*?)(?=진행\s*방식|이런|$)',
            r'난이도[:\s]*([^진행방식이런]*?)(?=진행\s*방식|이런|$)',
            r'경력[:\s]*([^진행방식이런]*?)(?=진행\s*방식|이런|$)',
        ]
        
        process_patterns = [
            r'진행\s*방식[:\s]*([^이런원하는]*?)(?=이런|원하는|$)',
            r'방식[:\s]*([^이런원하는]*?)(?=이런|원하는|$)',
            r'일정[:\s]*([^이런원하는]*?)(?=이런|원하는|$)',
        ]
        
        # 타겟 사용자 추출
        for pattern in target_user_patterns:
            match = re.search(pattern, content, re.IGNORECASE | re.DOTALL)
            if match:
                info['target_users'] = match.group(1).strip()[:200]
                break
        
        # 레벨 추출
        for pattern in level_patterns:
            match = re.search(pattern, content, re.IGNORECASE | re.DOTALL)
            if match:
                info['level'] = match.group(1).strip()[:150]
                break
        
        # 진행 방식 추출
        for pattern in process_patterns:
            match = re.search(pattern, content, re.IGNORECASE | re.DOTALL)
            if match:
                info['process'] = match.group(1).strip()[:200]
                break
        
        return info
    
    def infer_level_from_content(self, content: str) -> str:
        """내용에서 레벨 추정"""
        content_lower = content.lower()
        
        # 레벨 키워드 매칭
        beginner_keywords = ['초보', '입문', '기초', '처음', '신입', '비전공', '처음시작']
        intermediate_keywords = ['중급', '중간', '어느정도', '경험', '실무']
        advanced_keywords = ['고급', '심화', '전문', '시니어', '리드', '아키텍트']
        
        beginner_count = sum(1 for kw in beginner_keywords if kw in content_lower)
        intermediate_count = sum(1 for kw in intermediate_keywords if kw in content_lower)
        advanced_count = sum(1 for kw in advanced_keywords if kw in content_lower)
        
        if advanced_count > intermediate_count and advanced_count > beginner_count:
            return "고급 (시니어/전문가 수준)"
        elif intermediate_count > beginner_count:
            return "중급 (경험자 수준)"
        elif beginner_count > 0:
            return "초급 (입문자/초보자 수준)"
        else:
            return "수준 미지정"
    
    def summarize(self, content: str, max_length: int = 200) -> Dict:
        """구조화된 요약 생성"""
        if not content:
            return {
                'summary': '',
                'error': '본문이 비어있습니다.'
            }
        
        # 텍스트 정리
        cleaned_text = self.clean_text(content)
        original_length = len(cleaned_text)
        
        # 구조화된 정보 추출
        structured_info = self.extract_structured_info(cleaned_text)
        
        # 레벨 추정 (명시적으로 없으면 추정)
        if not structured_info['level']:
            structured_info['level'] = self.infer_level_from_content(cleaned_text)
        
        # 타겟 사용자 추출 (명시적으로 없으면 요약에서 추출)
        if not structured_info['target_users']:
            # 간단한 요약 생성
            sentences = self.extract_sentences(cleaned_text)
            if sentences:
                # 앞부분 문장들을 요약으로 사용
                summary_sentences = sentences[:3]
                structured_info['summary'] = ' '.join(summary_sentences)[:max_length]
            else:
                structured_info['summary'] = cleaned_text[:max_length]
        else:
            # 타겟 사용자가 있으면 그것을 요약으로 사용
            structured_info['summary'] = structured_info['target_users'][:max_length]
        
        # 구조화된 요약 생성
        summary_parts = []
        
        if structured_info['target_users']:
            summary_parts.append(f"• 사용자는 이런 사람을 원함:\n  {structured_info['target_users']}")
        elif structured_info['summary']:
            summary_parts.append(f"• 스터디 소개:\n  {structured_info['summary']}")
        
        if structured_info['level']:
            summary_parts.append(f"• 어떤 수준의 레벨로 추정됨:\n  {structured_info['level']}")
        
        if structured_info['process']:
            # 진행 방식은 간단히만
            process_summary = structured_info['process'][:100]
            if len(structured_info['process']) > 100:
                process_summary += "..."
            summary_parts.append(f"• 진행 방식:\n  {process_summary}")
        
        # 요약이 비어있으면 기본 요약 생성
        if not summary_parts:
            sentences = self.extract_sentences(cleaned_text)
            if sentences:
                summary_text = ' '.join(sentences[:2])[:max_length]
                if len(' '.join(sentences[:2])) > max_length:
                    summary_text = summary_text[:max_length-3] + '...'
                summary_parts.append(f"• 요약:\n  {summary_text}")
            else:
                summary_parts.append(f"• 요약:\n  {cleaned_text[:max_length]}")
        
        summary = '\n\n'.join(summary_parts)
        summary_length = len(summary)
        
        return {
            'summary': summary,
            'original_length': original_length,
            'summary_length': summary_length,
            'structured': {
                'target_users': structured_info['target_users'],
                'level': structured_info['level'],
                'process': structured_info['process']
            }
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

