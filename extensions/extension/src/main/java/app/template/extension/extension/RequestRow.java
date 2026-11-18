package app.template.extension.extension;

import java.util.Locale;

/** Formats values and timings for display in request lists. */
public final class RequestRow {
    static String name(String oid) {
        OidDictionary.Entry entry = OidDictionary.lookup(oid);
        return entry == null ? "Unknown request" : entry.name;
    }

    static String response(RequestRecord request) {
        if (request.outcome == RequestRecord.Outcome.TIMEOUT) return "Timed out";
        if (request.outcome == RequestRecord.Outcome.CANCELLED) return "Cancelled";
        byte[] response = request.response();
        if (request.outcome == RequestRecord.Outcome.ERROR && response == null) return "Request failed";
        String value = SnmpValueFormatter.format(response, request.oid);
        return value.trim().isEmpty() ? "Empty response" : value;
    }

    static String offset(long nanos) {
        return nanos < 0 ? "time unavailable" : String.format(Locale.ROOT, "%.3f s", nanos / 1e9);
    }

    static String duration(long nanos, boolean estimated) {
        return nanos < 0 ? "duration unavailable" :
                String.format(Locale.ROOT, "%s%.0f ms", estimated ? "~" : "", nanos / 1e6);
    }

    public static String format(RequestRecord request, long offsetNanos, long durationNanos, boolean estimated) {
        return offset(offsetNanos) + " · " + duration(durationNanos, estimated) + " · " +
                name(request.oid) + "\n" + response(request);
    }

    private RequestRow() {}
}
