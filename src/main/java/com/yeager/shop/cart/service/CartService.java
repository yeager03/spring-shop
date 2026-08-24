package com.yeager.shop.cart.service;

import com.yeager.shop.cart.dto.CartItemResponse;
import com.yeager.shop.cart.dto.CartResponse;
import com.yeager.shop.cart.dto.UpdateCartItemRequest;
import com.yeager.shop.cart.entity.Cart;
import com.yeager.shop.cart.entity.CartItem;
import com.yeager.shop.cart.repository.CartItemRepository;
import com.yeager.shop.cart.repository.CartRepository;
import com.yeager.shop.catalog.entity.Product;
import com.yeager.shop.catalog.repository.ProductRepository;
import com.yeager.shop.common.exception.InvalidOperationException;
import com.yeager.shop.common.exception.ResourceNotFoundException;
import com.yeager.shop.user.entity.User;
import com.yeager.shop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);

        if (cart == null) {
            return new CartResponse(
                    List.of(),
                    0,
                    BigDecimal.ZERO
            );
        }

        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getCartId());

        List<CartItemResponse> items = cartItems
                .stream()
                .map(this::toItemResponse)
                .toList();

        int totalQuantity = cartItems
                .stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        BigDecimal totalAmount = items
                .stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                items,
                totalQuantity,
                totalAmount
        );
    }

    @Transactional
    public CartItemResponse setItem(
            Long userId,
            Long productId,
            UpdateCartItemRequest request
    ) {
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Active product not found by id: " + productId)
                );

        int quantity = request.getQuantity();

        if (quantity > product.getStock()) {
            throw new InvalidOperationException("Requested quantity exceeds available stock");
        }

        User user = userRepository.findForUpdateById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(user));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getCartId(), productId)
                .orElse(null);

        if (item != null) {
            item.setQuantity(quantity);

            return toItemResponse(item);
        }

        CartItem newItem = new CartItem();

        newItem.setCart(cart);
        newItem.setProduct(product);
        newItem.setQuantity(quantity);

        CartItem savedItem = cartItemRepository.save(newItem);

        return toItemResponse(savedItem);
    }

    @Transactional
    public void deleteItem(Long userId, Long productId) {
        userRepository.findForUpdateById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);

        if (cart == null) {
            return;
        }

        cartItemRepository.deleteItem(cart.getCartId(), productId);
    }

    @Transactional
    public void clearCart(Long userId) {
        userRepository.findForUpdateById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found by id: " + userId)
                );

        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);

        if (cart == null) {
            return;
        }

        cartItemRepository.deleteAllItems(cart.getCartId());
    }

    private Cart createCart(User user) {
        Cart cart = new Cart();

        cart.setUser(user);

        return cartRepository.save(cart);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        Product product = item.getProduct();

        BigDecimal subtotal = product
                .getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        boolean available = product.isActive() && (product.getStock() >= item.getQuantity());

        return new CartItemResponse(
                product.getProductId(),
                product.getTitle(),
                product.getSlug(),
                product.getPrice(),
                item.getQuantity(),
                subtotal,
                product.getStock(),
                available
        );
    }
}
