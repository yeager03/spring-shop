# Order Feature Plan (customer-facing)

Scope decisions:
- List endpoint: **paginated** (`PagedResponse` + `PageMeta`, catalog style).
- Endpoints: **customer only** — checkout, list, details, cancel. Staff status management deferred.
- Stock: **decrement on checkout, restore on cancel** (pessimistic lock, re-validate).

No DB migration needed — `orders` / `order_items` already exist in `V1__initial_schema.sql`.
All DTOs are plain Lombok classes (no `record`).

---

## 1. Entity change — `order/entity/Order.java`

- [x] Add items collection with cascade + `@OrderBy("createdAt ASC")`:
  ```java
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<OrderItem> items = new ArrayList<>();
  ```
- [x] Add `addItem(OrderItem item)` helper that sets both sides of the association.
- [x] Leave `OrderItem` and `OrderStatus` unchanged.

## 2. Repository changes

### `order/repository/OrderRepository.java`
- [x] Keep `findByIdAndUserId`.
- [x] Replace `List<Order> findByUserId(...)` with `Page<Order> findPageByUserId(Long userId, Pageable pageable)` (`@Query` on `o.user.userId = :userId`, auto count query).
- [x] Add `findDetailsByIdAndUserId` — `JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.orderId = :orderId AND o.user.userId = :userId`.

### `order/repository/OrderItemRepository.java`
- [x] Add a `JOIN FETCH oi.product` variant of `findByOrderId` (for stock restore on cancel).

### `catalog/repository/ProductRepository.java`
- [x] Add pessimistic-lock bulk lookup:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.productId IN :ids")
  List<Product> findForUpdateByIds(@Param("ids") Collection<Long> ids);
  ```

## 3. DTOs — `order/dto/`

- [x] `CreateOrderRequest` — `recipientName`, `recipientPhone`, `country`, `city`, `street`, `house` (`@NotBlank` + `@Size`); `apartment`, `postalCode` (optional, `@Size`). Message keys externalized.
- [x] `OrderListQuery` — `page` (`@Min 1`, default 1), `limit` (`@Min 1 @Max 20`, default 20). Mirrors `ProductListQuery`.
- [x] `OrderItemResponse` — `productId` (nullable), `productTitle`, `unitPrice`, `quantity`, `subtotal`.
- [x] `OrderResponse` (list row) — `orderId`, `status`, `totalAmount`, `totalQuantity`, `itemCount`, `createdAt`.
- [x] `OrderDetailsResponse` — order id, `status`, `totalAmount`, `totalQuantity`, recipient + address fields, `List<OrderItemResponse> items`, `createdAt`, `updatedAt`.

## 4. Service — `order/service/OrderService.java`

`@Service @RequiredArgsConstructor`; injects `OrderRepository`, `OrderItemRepository`, `CartRepository`,
`CartItemRepository`, `ProductRepository`, `UserRepository`.

- [x] `checkout(Long userId, CreateOrderRequest request)` `@Transactional`:
  1. `userRepository.findForUpdateById` → `ResourceNotFoundException` if missing.
  2. Load cart (`cartRepository.findByUserId`) + items (`findAllByCartId`, join-fetches product); empty → `InvalidOperationException("Cart is empty")`.
  3. Lock referenced products via `findForUpdateByIds` (ids sorted to avoid deadlock) into a map.
  4. Per item: product active and `stock >= quantity`, else `InvalidOperationException` naming the product.
  5. Build `Order` (status `CREATED`, recipient/address from request); one `OrderItem` per cart item snapshotting `productTitle` + `unitPrice = product.getPrice()`; `totalAmount = Σ unitPrice·qty`.
  6. Decrement `product.stock`.
  7. `orderRepository.save(order)` (cascades items); `cartItemRepository.deleteAllItems(cartId)`.
  8. Return `OrderDetailsResponse`.
- [x] `getOrders(Long userId, OrderListQuery query)` `@Transactional(readOnly = true)` → `PagedResponse<OrderResponse>` with `PageMeta`, `PageRequest.of(page - 1, limit, Sort.by(DESC, "createdAt"))`, `appliedQuery` map with page/limit.
- [x] `getOrder(Long userId, Long orderId)` `readOnly` → `findDetailsByIdAndUserId` → `ResourceNotFoundException` else `OrderDetailsResponse`.
- [x] `cancelOrder(Long userId, Long orderId)` `@Transactional`:
  - `findByIdAndUserId` → 404.
  - Status must be `CREATED` or `PAID`, else `InvalidOperationException("Order cannot be cancelled in status " + status)`.
  - Set `CANCELLED`; load items with product, lock those products, increment stock for items whose `product != null`.

## 5. Controller — `order/controller/OrderController.java`

`@RestController @RequestMapping("/orders")`; current user via `@AuthenticationPrincipal AuthenticatedUserPrincipal principal`.

- [x] `POST /orders` → 201, `@Valid @RequestBody CreateOrderRequest` → `OrderDetailsResponse`.
- [x] `GET /orders` → `@Valid @ModelAttribute OrderListQuery` → `PagedResponse<OrderResponse>`.
- [x] `GET /orders/{orderId}` → `@Positive` path var → `OrderDetailsResponse`.
- [x] `POST /orders/{orderId}/cancel` → 204.
- [x] No `SecurityConfig` change — `/orders/**` is covered by `anyRequest().authenticated()`; ownership enforced in the service by `userId`.

## 6. `src/main/resources/messages.properties`

- [x] Add an `# order` section: `CreateOrderRequest` field constraint keys, `order.common.id.positive`, `order.list.page.min`, `order.list.limit.min`, `order.list.limit.max`, `typeMismatch.orderListQuery.*`.

## 7. Tests

- [x] `order/service/OrderServiceTest.java` (`@ExtendWith(MockitoExtension.class)`): checkout success (stock decremented, cart cleared, totals), empty cart → `InvalidOperationException`, insufficient stock, inactive product, `getOrder` not found, cancel success (stock restored), cancel in non-cancellable status, cancel order not found.
- [x] `order/controller/OrderControllerTest.java` (`@WebMvcTest(OrderController.class)` + `@AutoConfigureMockMvc(addFilters = false)` + `@MockitoBean OrderService`): 201 on checkout, `ProblemDetail` shape for empty-cart 400, 404 details, validation `errors` array for a blank recipient field, limit-exceeds-max 400, paged list 200, cancel 204.

## 8. Verify

- [x] `./mvnw test -Dtest=OrderServiceTest,OrderControllerTest` — **16 tests pass** (9 service + 7 controller).
- [x] `./mvnw clean package -Dmaven.test.skip=true` — main sources compile and jar builds.
- [x] `./mvnw clean package` (full test run) — **BUILD SUCCESS, 28 tests pass**. The pre-existing
  `catalog` test breakage (`ProductServiceTest` / `ProductControllerTest` vs. the updated
  `ProductImageResponse` / `ProductListItemResponse` DTOs) has been fixed.

---

### New vs modified files

**New:** `order/dto/CreateOrderRequest.java`, `order/dto/OrderListQuery.java`, `order/dto/OrderItemResponse.java`,
`order/dto/OrderResponse.java`, `order/dto/OrderDetailsResponse.java`, `order/service/OrderService.java`,
`order/controller/OrderController.java`, `order/service/OrderServiceTest.java`, `order/controller/OrderControllerTest.java`.

**Modified:** `order/entity/Order.java`, `order/repository/OrderRepository.java`, `order/repository/OrderItemRepository.java`,
`catalog/repository/ProductRepository.java`, `src/main/resources/messages.properties`.
