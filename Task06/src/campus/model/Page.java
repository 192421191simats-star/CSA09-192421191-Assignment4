package campus.model;

import java.util.List;

/** One page of registrations plus the total, so the UI can show "Page x of y". */
public record Page(List<Registration> rows, int pageIndex, int pageSize, int totalRows) {
    public int pageCount() { return Math.max(1, (totalRows + pageSize - 1) / pageSize); }
    public boolean hasPrevious() { return pageIndex > 0; }
    public boolean hasNext() { return pageIndex + 1 < pageCount(); }
}
