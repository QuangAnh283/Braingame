package org.example.quizizz.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public class PageableUtil {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE_NUMBER = 10_000;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "title",
            "username",
            "email",
            "fullName",
            "roleName",
            "permissionName",
            "questionText",
            "answerText",
            "createdAt",
            "updatedAt"
    );
    
    public static Pageable createPageable(int page, int size, String sort) {
        int sanitizedPage = Math.max(0, Math.min(page, MAX_PAGE_NUMBER));
        int sanitizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        if (sort == null || sort.isBlank()) {
            sort = "id,desc";
        }

        String[] sortParams = sort.split(",", 2);
        String sortField = ALLOWED_SORT_FIELDS.contains(sortParams[0]) ? sortParams[0] : "id";
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        return PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(direction, sortField));
    }
}
