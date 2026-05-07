package com.flashlearn.backend.category;

import com.flashlearn.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Zwraca liste wszystkich kategorii.
     * Endpoint publiczny — Android uzywa do wypelnienia dropdownu
     * przy tworzeniu talii i filtrowania w Marketplace.
     *
     * @return lista kategorii
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getIconName()))
                .toList();
    }
}