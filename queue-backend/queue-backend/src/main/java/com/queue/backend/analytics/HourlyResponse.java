package com.queue.backend.analytics;

/** Broj izdatih brojeva u jednom satu (0-23). Sati bez ticketa se ne izostavljaju - grafikon hoce svih 24. */
public record HourlyResponse(int hour, long issued) {
}
