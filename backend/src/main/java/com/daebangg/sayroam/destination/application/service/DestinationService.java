package com.daebangg.sayroam.destination.application.service;

import com.daebangg.sayroam.destination.application.dto.DestinationResult;
import com.daebangg.sayroam.destination.domain.DestinationRepository;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DestinationService {
    public static final int MAX_QUERY_LENGTH = 80;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private final DestinationRepository destinations;

    @Transactional(readOnly = true)
    public List<DestinationResult> findDestinations(@Nullable String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        String searchText = query == null ? "" : WHITESPACE.matcher(query).replaceAll("");
        return destinations.findActiveByName(searchText).stream().map(DestinationResult::from).toList();
    }
}
