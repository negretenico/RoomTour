package com.roomtour.voice.stt;

import com.fasterxml.jackson.annotation.JsonProperty;

record WhisperSegment(@JsonProperty("avg_logprob") double avgLogprob) {}
