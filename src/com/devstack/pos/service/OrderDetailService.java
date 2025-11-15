package com.devstack.pos.service;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.repository.OrderDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDetailService {
    
    private final OrderDetailRepository orderDetailRepository;
    
    public OrderDetail saveOrderDetail(OrderDetail orderDetail) {
        return orderDetailRepository.save(orderDetail);
    }
    
    public boolean updateOrderDetail(OrderDetail orderDetail) {
        if (orderDetailRepository.existsById(orderDetail.getCode())) {
            orderDetailRepository.save(orderDetail);
            return true;
        }
        return false;
    }
    
    public boolean deleteOrderDetail(Long code) {
        if (orderDetailRepository.existsById(code)) {
            orderDetailRepository.deleteById(code);
            return true;
        }
        return false;
    }
    
    public OrderDetail findOrderDetail(Long code) {
        return orderDetailRepository.findById(code).orElse(null);
    }
    
    public List<OrderDetail> findAllOrderDetails() {
        return orderDetailRepository.findAll();
    }
    
    public List<OrderDetail> findByCustomerId(Long customerId) {
        return orderDetailRepository.findByCustomerId(customerId);
    }
    
    public List<OrderDetail> findByCustomerName(String customerName) {
        return orderDetailRepository.findByCustomerNameContaining(customerName);
    }
    
    @Transactional(readOnly = true)
    public Double getTotalSpentByCustomerId(Long customerId) {
        if (customerId == null) {
            return 0.0;
        }
        Double total = orderDetailRepository.getTotalSpentByCustomerId(customerId);
        return total != null ? total : 0.0;
    }
    
    // Analytics methods
    @Transactional(readOnly = true)
    public Double getTotalRevenue() {
        Double revenue = orderDetailRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getAverageOrderValue() {
        Double avg = orderDetailRepository.getAverageOrderValue();
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Long getTotalOrderCount() {
        return orderDetailRepository.getTotalOrderCount();
    }
    
    @Transactional(readOnly = true)
    public Double getRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderDetailRepository.getRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersByRevenue() {
        return orderDetailRepository.getTopCustomersByRevenue();
    }
    
    @Transactional(readOnly = true)
    public List<OrderDetail> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.findOrdersByDateRange(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCashier() {
        return orderDetailRepository.getSalesByCashier();
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCashierByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getSalesByCashierByDateRange(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersWithOrderCount() {
        return orderDetailRepository.getTopCustomersWithOrderCount();
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersWithOrderCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getTopCustomersWithOrderCountByDateRange(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public Long countOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long count = orderDetailRepository.countOrdersByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Double getAverageOrderValueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double avg = orderDetailRepository.getAverageOrderValueByDateRange(startDate, endDate);
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public List<OrderDetail> findPendingPayments() {
        return orderDetailRepository.findPendingPayments();
    }
    
    @Transactional(readOnly = true)
    public List<OrderDetail> findPendingPaymentsByMethod(String paymentMethod) {
        return orderDetailRepository.findPendingPaymentsByMethod(paymentMethod);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getPendingPaymentsByCustomer() {
        return orderDetailRepository.getPendingPaymentsByCustomer();
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getPendingPaymentsByCustomerByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getPendingPaymentsByCustomerByDateRange(startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public Double getPendingPaymentsTotalByCustomerId(Long customerId) {
        Double result = orderDetailRepository.getPendingPaymentsTotalByCustomerId(customerId);
        return result != null ? result : 0.0;
    }
    
    @Transactional
    public boolean completePayment(Long orderCode) {
        OrderDetail order = findOrderDetail(orderCode);
        if (order != null && "PENDING".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
            orderDetailRepository.save(order);
            return true;
        }
        return false;
    }
    
    // Construction-specific methods
    @Transactional(readOnly = true)
    public Double getRevenueByOrderType(String orderType) {
        Double revenue = orderDetailRepository.getRevenueByOrderType(orderType);
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getRevenueByOrderTypeAndDateRange(String orderType, LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderDetailRepository.getRevenueByOrderTypeAndDateRange(orderType, startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Long getOrderCountByOrderType(String orderType) {
        Long count = orderDetailRepository.getOrderCountByOrderType(orderType);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Long getOrderCountByOrderTypeAndDateRange(String orderType, LocalDateTime startDate, LocalDateTime endDate) {
        Long count = orderDetailRepository.getOrderCountByOrderTypeAndDateRange(orderType, startDate, endDate);
        return count != null ? count : 0L;
    }
    
    @Transactional(readOnly = true)
    public Double getAverageOrderValueByOrderType(String orderType) {
        Double avg = orderDetailRepository.getAverageOrderValueByOrderType(orderType);
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public Double getAverageOrderValueByOrderTypeAndDateRange(String orderType, LocalDateTime startDate, LocalDateTime endDate) {
        Double avg = orderDetailRepository.getAverageOrderValueByOrderTypeAndDateRange(orderType, startDate, endDate);
        return avg != null ? avg : 0.0;
    }
    
    @Transactional(readOnly = true)
    public List<OrderDetail> findOrdersByOrderTypeAndDateRange(String orderType, LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.findOrdersByOrderTypeAndDateRange(orderType, startDate, endDate);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCashierByOrderType(String orderType) {
        return orderDetailRepository.getSalesByCashierByOrderType(orderType);
    }
    
    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCashierByOrderTypeAndDateRange(String orderType, LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getSalesByCashierByOrderTypeAndDateRange(orderType, startDate, endDate);
    }
}

