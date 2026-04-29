import { useEffect, useState } from 'react';

/**
 * useDebounce - Trả về giá trị "debounced" sau khoảng `delay` ms không có thay đổi mới.
 * Dùng cho ô tìm kiếm để tránh gọi API mỗi lần gõ phím.
 *
 * @param value Giá trị nguyên thủy cần debounce (string, number, ...)
 * @param delay Khoảng chờ (ms) trước khi nhận giá trị mới. Mặc định 500ms.
 * @returns Giá trị đã debounce
 *
 * @example
 *   const debouncedKeyword = useDebounce(searchTerm, 500);
 *   useEffect(() => { fetchData(debouncedKeyword); }, [debouncedKeyword]);
 */
export function useDebounce<T>(value: T, delay = 500): T {
    const [debounced, setDebounced] = useState<T>(value);

    useEffect(() => {
        const timer = setTimeout(() => setDebounced(value), delay);
        return () => clearTimeout(timer);
    }, [value, delay]);

    return debounced;
}

export default useDebounce;
