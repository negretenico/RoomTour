package com.roomtour.voice;

/**
 * Raw PCM audio with its sample rate.
 * Shared between the STT (mic capture) and TTS (synthesis) domains.
 */
public record AudioChunk(byte[] pcm, int sampleRateHz) {}
