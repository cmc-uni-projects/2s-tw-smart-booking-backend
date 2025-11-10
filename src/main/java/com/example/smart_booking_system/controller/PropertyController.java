package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.service.PropertyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PropertyController {
    private final PropertyService propertyService;
    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    public String addProperty(@ModelAttribute ) {

    }
}
