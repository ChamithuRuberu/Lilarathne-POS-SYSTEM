package com.devstack.pos.service;

import com.devstack.pos.entity.ItemDetail;
import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.repository.OrderDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDetailService {
    
    private final OrderDetailRepository orderDetailRepository;
    
    public OrderDetail saveOrderDetail(OrderDetail orderDetail) {
        return orderDetailRepository.save(orderDetail);
    }
    
    public boolean updateOrderDetail(OrderDetail orderDetail) {
        if (orderDetailRepository.existsById(Math.toIntExact(orderDetail.getCode()))) {
            orderDetailRepository.save(orderDetail);
            return true;
        }
        return false;
    }
    
    public boolean deleteOrderDetail(Integer code) {
        if (orderDetailRepository.existsById(code)) {
            orderDetailRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    public OrderDetail findOrderDetail(Integer code) {
        return orderDetailRepository.findById(code).orElse(null);
    }
    
    public List<OrderDetail> findAllOrderDetails() {
        return orderDetailRepository.findAll();
    }
    
    public List<OrderDetail> findByCustomerEmail(String email) {
        return orderDetailRepository.findByCustomerEmail(email);
    }
    
    public OrderDetail createOrder(OrderDetail orderDetail, List<ItemDetail> itemDetails) {
        // Save order first
        OrderDetail savedOrder = orderDetailRepository.save(orderDetail);
        
        // Set order code for all item details
        for (ItemDetail itemDetail : itemDetails) {
            itemDetail.setOrderCode(Math.toIntExact(savedOrder.getCode()));
        }
        
        return savedOrder;
    }
}

