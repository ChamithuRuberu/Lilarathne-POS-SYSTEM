package com.devstack.pos.service;

import com.devstack.pos.entity.Category;
import com.devstack.pos.enums.Status;
import com.devstack.pos.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    public Category saveCategory(Category category) {
        if (category.getStatus() == null) {
            category.setStatus(Status.ACTIVE);
        }
        
        // Check if category name already exists
        if (categoryRepository.existsByName(category.getName())) {
            throw new IllegalArgumentException("Category name already exists.");
        }
        
        return categoryRepository.save(category);
    }
    
    public boolean updateCategory(Category category) {
        if (categoryRepository.existsById(category.getId())) {
            categoryRepository.save(category);
            return true;
        }
        return false;
    }
    
    public boolean deleteCategory(Integer id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public Category findCategory(Integer id) {
        return categoryRepository.findById(id).orElse(null);
    }
    
    public Category findCategoryByName(String name) {
        return categoryRepository.findByName(name).orElse(null);
    }
    
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }
    
    public List<Category> findActiveCategories() {
        return categoryRepository.findByStatus(Status.ACTIVE);
    }
}

