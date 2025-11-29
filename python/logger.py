#!/usr/bin/env python3
"""
로깅 설정 모듈
"""

import logging
import sys
from config import Config


def setup_logger(name: str, level: Optional[str] = None) -> logging.Logger:
    """
    로거 설정
    
    Args:
        name: 로거 이름
        level: 로그 레벨 (None이면 Config에서 가져옴)
    
    Returns:
        설정된 로거
    """
    logger = logging.getLogger(name)
    
    # 레벨 설정
    if level is None:
        level = Config.LOG_LEVEL
    
    log_level = getattr(logging, level.upper(), logging.INFO)
    logger.setLevel(log_level)
    
    # 이미 핸들러가 있으면 추가하지 않음
    if logger.handlers:
        return logger
    
    # 콘솔 핸들러
    handler = logging.StreamHandler(sys.stderr)
    handler.setLevel(log_level)
    
    # 포맷터
    formatter = logging.Formatter(
        Config.LOG_FORMAT,
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    handler.setFormatter(formatter)
    
    logger.addHandler(handler)
    
    return logger


