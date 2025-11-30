#!/usr/bin/env python3
"""
AI 기반 게시글 요약 시스템
게시글 본문을 분석하여 간결한 요약을 생성합니다.

알고리즘:
1. TF-IDF (Term Frequency-Inverse Document Frequency): 키워드 중요도 기반 요약
2. TextRank: 그래프 기반 문장 중요도 계산
3. 규칙 기반 추출: 구조화된 정보 추출
"""

import json
import sys
import re
import math
from typing import Dict, List, Tuple, Set
from collections import Counter, defaultdict


class TFIDFSummarizer:
    """TF-IDF 기반 요약 알고리즘"""
    
    def __init__(self):
        self.stop_words = {'은', '는', '이', '가', '을', '를', '의', '에', '에서', '와', '과', '도', '로', '으로', 
                          '그', '그것', '이것', '저것', '이런', '그런', '저런', '그리고', '또한', '또', '하지만',
                          '그러나', '그래서', '따라서', '그런데', '그럼', '그렇다면', '만약', '만약에', '만일',
                          '때문에', '위해', '위하여', '통해', '통하여', '대해', '대하여', '관해', '관하여',
                          '있', '없', '하', '되', '되다', '하다', '있다', '없다', '되다', '이다', '아니다'}
    
    def extract_keywords(self, text: str, top_n: int = 10) -> List[Tuple[str, float]]:
        """TF-IDF로 키워드 추출"""
        # 단어 분리 (한글, 영문, 숫자)
        words = re.findall(r'[가-힣]+|[a-zA-Z]+|\d+', text.lower())
        
        # 불용어 제거 및 단어 빈도 계산
        word_freq = Counter()
        for word in words:
            if len(word) >= 2 and word not in self.stop_words:
                word_freq[word] += 1
        
        # TF 계산 (문서 내 빈도)
        total_words = sum(word_freq.values())
        tf_scores = {word: count / total_words for word, count in word_freq.items()}
        
        # 간단한 IDF 계산 (문서가 하나이므로 단순화)
        # 실제로는 여러 문서가 있어야 하지만, 여기서는 단어 길이와 빈도를 고려
        idf_scores = {}
        for word, count in word_freq.items():
            # 단어 길이와 빈도를 고려한 가중치
            length_weight = len(word) / 10.0  # 최대 10글자 가정
            freq_weight = math.log(1 + count)
            idf_scores[word] = length_weight * freq_weight
        
        # TF-IDF 점수 계산
        tfidf_scores = {word: tf_scores[word] * idf_scores[word] 
                       for word in word_freq.keys()}
        
        # 상위 키워드 반환
        top_keywords = sorted(tfidf_scores.items(), key=lambda x: x[1], reverse=True)[:top_n]
        return top_keywords
    
    def score_sentences(self, sentences: List[str], keywords: List[Tuple[str, float]]) -> List[float]:
        """문장 점수 계산 (키워드 포함 여부)"""
        keyword_dict = {word: score for word, score in keywords}
        scores = []
        
        for sentence in sentences:
            score = 0.0
            words = re.findall(r'[가-힣]+|[a-zA-Z]+|\d+', sentence.lower())
            
            for word in words:
                if word in keyword_dict:
                    score += keyword_dict[word]
            
            # 문장 길이 정규화
            if len(sentence) > 0:
                score = score / math.sqrt(len(sentence))
            
            scores.append(score)
        
        return scores


class TextRankSummarizer:
    """TextRank 기반 요약 알고리즘"""
    
    def __init__(self, damping_factor: float = 0.85, max_iter: int = 100):
        self.damping_factor = damping_factor
        self.max_iter = max_iter
    
    def build_similarity_matrix(self, sentences: List[str]) -> List[List[float]]:
        """문장 간 유사도 행렬 구축"""
        n = len(sentences)
        similarity_matrix = [[0.0] * n for _ in range(n)]
        
        for i in range(n):
            for j in range(n):
                if i != j:
                    similarity = self._sentence_similarity(sentences[i], sentences[j])
                    similarity_matrix[i][j] = similarity
        
        return similarity_matrix
    
    def _sentence_similarity(self, sent1: str, sent2: str) -> float:
        """두 문장 간 유사도 계산 (단어 공통 비율)"""
        words1 = set(re.findall(r'[가-힣]+|[a-zA-Z]+', sent1.lower()))
        words2 = set(re.findall(r'[가-힣]+|[a-zA-Z]+', sent2.lower()))
        
        if len(words1) == 0 or len(words2) == 0:
            return 0.0
        
        intersection = len(words1 & words2)
        union = len(words1 | words2)
        
        return intersection / union if union > 0 else 0.0
    
    def calculate_textrank_scores(self, similarity_matrix: List[List[float]]) -> List[float]:
        """TextRank 점수 계산 (PageRank 알고리즘)"""
        n = len(similarity_matrix)
        scores = [1.0] * n
        
        # 각 노드의 나가는 가중치 합 계산
        out_weights = [sum(row) for row in similarity_matrix]
        
        for _ in range(self.max_iter):
            new_scores = [0.0] * n
            
            for i in range(n):
                score = 0.0
                for j in range(n):
                    if i != j and out_weights[j] > 0:
                        score += scores[j] * (similarity_matrix[j][i] / out_weights[j])
                
                new_scores[i] = (1 - self.damping_factor) + self.damping_factor * score
            
            # 수렴 확인
            if max(abs(new_scores[i] - scores[i]) for i in range(n)) < 0.0001:
                break
            
            scores = new_scores
        
        return scores
    
    def summarize(self, sentences: List[str], top_n: int = 3) -> List[Tuple[int, str, float]]:
        """TextRank로 요약"""
        if len(sentences) <= top_n:
            return [(i, sent, 1.0) for i, sent in enumerate(sentences)]
        
        similarity_matrix = self.build_similarity_matrix(sentences)
        scores = self.calculate_textrank_scores(similarity_matrix)
        
        # 점수와 인덱스 결합
        scored_sentences = [(i, sentences[i], scores[i]) for i in range(len(sentences))]
        scored_sentences.sort(key=lambda x: x[2], reverse=True)
        
        # 상위 N개 선택 후 원래 순서대로 정렬
        top_sentences = scored_sentences[:top_n]
        top_sentences.sort(key=lambda x: x[0])
        
        return top_sentences


class AISummarizer:
    """AI 요약기 (하이브리드: TF-IDF + TextRank + 규칙 기반)"""
    
    def __init__(self):
        self.max_length = 200  # 최대 요약 길이
        self.tfidf_summarizer = TFIDFSummarizer()
        self.textrank_summarizer = TextRankSummarizer()
    
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
    
    def summarize(self, content: str, max_length: int = 200, method: str = 'hybrid') -> Dict:
        """
        구조화된 요약 생성
        
        Args:
            content: 원본 텍스트
            max_length: 최대 요약 길이
            method: 'tfidf', 'textrank', 'hybrid' 중 선택
        """
        if not content:
            return {
                'summary': '',
                'error': '본문이 비어있습니다.',
                'method': method
            }
        
        # 텍스트 정리
        cleaned_text = self.clean_text(content)
        original_length = len(cleaned_text)
        
        # 구조화된 정보 추출
        structured_info = self.extract_structured_info(cleaned_text)
        
        # 레벨 추정
        if not structured_info['level']:
            structured_info['level'] = self.infer_level_from_content(cleaned_text)
        
        # 문장 추출
        sentences = self.extract_sentences(cleaned_text)
        
        # 알고리즘 기반 요약 생성
        algorithm_summary = ""
        if method in ['tfidf', 'hybrid'] and len(sentences) > 1:
            # TF-IDF 기반 요약
            keywords = self.tfidf_summarizer.extract_keywords(cleaned_text, top_n=10)
            sentence_scores = self.tfidf_summarizer.score_sentences(sentences, keywords)
            
            # 상위 문장 선택
            scored_sentences = [(i, sentences[i], sentence_scores[i]) 
                               for i in range(len(sentences))]
            scored_sentences.sort(key=lambda x: x[2], reverse=True)
            
            # 상위 3개 문장 선택 후 원래 순서대로 정렬
            top_indices = sorted([i for i, _, _ in scored_sentences[:3]])
            tfidf_summary = ' '.join([sentences[i] for i in top_indices])
            algorithm_summary = tfidf_summary
        
        if method == 'textrank' and len(sentences) > 1:
            # TextRank 기반 요약
            top_sentences = self.textrank_summarizer.summarize(sentences, top_n=3)
            textrank_summary = ' '.join([sent for _, sent, _ in top_sentences])
            algorithm_summary = textrank_summary
        
        if method == 'hybrid' and len(sentences) > 1:
            # 하이브리드: TF-IDF와 TextRank 결합
            keywords = self.tfidf_summarizer.extract_keywords(cleaned_text, top_n=10)
            tfidf_scores = self.tfidf_summarizer.score_sentences(sentences, keywords)
            
            similarity_matrix = self.textrank_summarizer.build_similarity_matrix(sentences)
            textrank_scores = self.textrank_summarizer.calculate_textrank_scores(similarity_matrix)
            
            # 두 점수 결합 (가중 평균)
            combined_scores = [
                0.5 * tfidf_scores[i] + 0.5 * textrank_scores[i]
                for i in range(len(sentences))
            ]
            
            # 상위 문장 선택
            scored_sentences = [(i, sentences[i], combined_scores[i]) 
                               for i in range(len(sentences))]
            scored_sentences.sort(key=lambda x: x[2], reverse=True)
            
            top_indices = sorted([i for i, _, _ in scored_sentences[:3]])
            hybrid_summary = ' '.join([sentences[i] for i in top_indices])
            algorithm_summary = hybrid_summary
        
        # 타겟 사용자 추출
        if not structured_info['target_users']:
            if algorithm_summary:
                structured_info['summary'] = algorithm_summary[:max_length]
            else:
                # 폴백: 앞부분 문장들
                if sentences:
                    summary_sentences = sentences[:3]
                    structured_info['summary'] = ' '.join(summary_sentences)[:max_length]
                else:
                    structured_info['summary'] = cleaned_text[:max_length]
        else:
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
            process_summary = structured_info['process'][:100]
            if len(structured_info['process']) > 100:
                process_summary += "..."
            summary_parts.append(f"• 진행 방식:\n  {process_summary}")
        
        # 요약이 비어있으면 기본 요약 생성
        if not summary_parts:
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
            'method': method,
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

