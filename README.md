# checkout
# 🛍️ E-Commerce Checkout Cart Service

A **robust, scalable shopping cart microservice** built with **Spring Boot** that supports **idempotent operations**, **multi-tenant handling**, and **pluggable storage** (MongoDB or in-memory).  
Ideal for modern e-commerce platforms and payment systems (e.g. Redcare-style).

---

## 🚀 Features

| Category | Description |
|-----------|-------------|
| 🛒 **Cart Management** | Create, read, update, and delete shopping carts |
| 👤 **Multi-User Support** | Handles both authenticated users and anonymous guests |
| 🔄 **Idempotent Operations** | Prevent duplicate operations with idempotency keys |
| 💾 **Flexible Storage** | In-memory for dev, MongoDB for production |
| 📚 **API Documentation** | Complete Swagger/OpenAPI documentation |
| 🔒 **Security Ready** | OAuth2 integration prepared (disabled in dev) |
| 🎯 **Optimistic Locking** | Prevents concurrent modification conflicts |
| 🛡️ **Validation** | Comprehensive field-level validation |
| 🌍 **International** | Multi-currency and multi-country support |

---

## 🏗️ Architecture

```text
Client Apps
     │
     ▼
 Cart Controller (REST API)
     │
     ▼
 Cart Service (Business Logic)
     │
     ▼
 Repository Layer
     │
     ├── In-Memory Storage (Development)
     └── MongoDB Storage (Production)
