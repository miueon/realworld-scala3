# Real World Scala 3 🚀

[![Scala](https://img.shields.io/badge/Scala-3.6.2-red.svg)](https://scala-lang.org)
[![Smithy4s](https://img.shields.io/badge/Smithy4s-Contract--First-blue.svg)](https://smithy4s.dev)
[![Http4s](https://img.shields.io/badge/Http4s-Functional-green.svg)](https://http4s.org)
[![Laminar](https://img.shields.io/badge/Laminar-Reactive-purple.svg)](https://laminar.dev)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **A showcase implementation of the RealWorld application using cutting-edge Scala 3 technologies**

This project demonstrates a **full-stack, type-safe Scala 3 application** built with modern functional programming principles. It serves as both a real-world example and a reference architecture for building scalable, maintainable applications in the Scala ecosystem.

## 🌟 What Makes This Special

### **Cross-Platform Architecture**
- **Unified Codebase**: Shared domain models and API definitions between JVM backend and Scala.js frontend
- **Type Safety Everywhere**: From PostgreSQL queries to UI components, leveraging Scala's type system
- **Contract-First Development**: Smithy4s generates type-safe client/server code from API specifications

### **Modern Scala 3 Features**
- **Latest Scala 3.6.2** with experimental features enabled
- **Opaque Types** for type-safe domain modeling
- **Iron Refinement Types** for compile-time validation
- **Experimental Better-For** syntax for enhanced readability

### **Production-Ready Stack**
- **Functional Architecture**: Built on Cats Effect ecosystem for pure functional programming
- **Infrastructure as Code**: Pulumi + Kubernetes deployment with monitoring
- **Comprehensive Testing**: Unit, integration, and E2E tests with Playwright
- **Developer Experience**: Hot reloading, Just task runner, automated workflows

## 🛠 Technology Stack

### Backend (JVM)
| Technology | Purpose | Why This Choice |
|------------|---------|----------------|
| **Http4s** | HTTP Server/Client | Pure functional, composable HTTP |
| **Doobie** | Database Access | Type-safe SQL with PostgreSQL |
| **Smithy4s** | API Specification | Contract-first, cross-platform |
| **Cats Effect** | Effect System | Pure functional async/concurrent programming |
| **Iron** | Refinement Types | Compile-time validation and safety |
| **Ciris** | Configuration | Type-safe configuration management |
| **Redis4Cats** | Caching | Functional Redis client |

### Frontend (Scala.js)
| Technology | Purpose | Why This Choice |
|------------|---------|----------------|
| **Laminar** | UI Framework | Reactive, type-safe UI with FRP |
| **Waypoint** | Routing | Type-safe routing for SPAs |
| **Monocle** | Optics | Functional state management |
| **Vite** | Build Tool | Fast development with HMR |

### DevOps & Infrastructure
| Technology | Purpose | Why This Choice |
|------------|---------|----------------|
| **Pulumi** | Infrastructure | Type-safe infrastructure as code |
| **Kubernetes** | Container Orchestration | Production-grade deployment |
| **TestContainers** | Integration Testing | Isolated test environments |
| **Playwright** | E2E Testing | Modern browser automation |

## 🏗 Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │  Infrastructure │
│  (Scala.js)     │    │    (JVM)        │    │                 │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • Laminar UI    │◄──►│ • Http4s Server │◄──►│ • PostgreSQL    │
│ • Waypoint      │    │ • Doobie DB     │    │ • Redis Cache   │
│ • Type-safe API │    │ • JWT Auth      │    │ • Kubernetes    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
           │                       │                       │
           └───────────────────────┼───────────────────────┘
                                   │
                    ┌─────────────────┐
                    │  Shared Code    │
                    │                 │
                    │ • Smithy Specs  │
                    │ • Domain Models │
                    │ • Type Defs     │
                    └─────────────────┘
```

### **Multi-Module Structure**
- `modules/shared/` - Cross-platform domain models and API specifications
- `modules/backend/` - JVM server implementation with business logic
- `modules/frontend/` - Scala.js client application
- `modules/app/` - Application assembly and deployment artifacts
- `infra/` - Infrastructure as Code (Pulumi + Kubernetes)

## 🚀 Quick Start

### Prerequisites
- **Java 17+** (JDK)
- **Docker & Docker Compose** (for databases)
- **Just Task Runner** (`cargo install just` or package manager)
- **pnpm** (for frontend dependencies)

### **1. Clone and Setup**
```bash
git clone https://github.com/your-username/real-world-scala3.git
cd real-world-scala3

# Start databases
docker compose up -d

# Install frontend dependencies  
just install-frontend
```

### **2. Development Mode**
```bash
# Start full development environment (recommended)
just dev

# Or start services individually:
just dev-scala     # Backend with hot reload
just dev-js        # Frontend dev server  
just dev-scala-js  # Scala.js compilation
```

### **3. Access the Application**
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/docs

## 💻 Development Workflow

### **Essential Commands**
```bash
# Development
just dev                    # Full development environment
just dev-scala             # Backend development server  
just dev-js                # Frontend development server
just dev-scala-js          # Watch-compile Scala.js

# Building
just build-and-copy-frontend  # Production frontend build
just publish-docker           # Docker image build & publish

# Testing  
just test                   # Run backend tests
sbt 'backend/test'         # Direct SBT test execution
```

### **Project Conventions**
- **Type Safety First**: Extensive use of refined types and opaque types
- **Functional Core**: Pure functions with effects at the boundaries
- **Contract-First**: API-first development with Smithy specifications
- **Self-Documenting Code**: Minimal comments, expressive types and names

### **Hot Reloading**
- **Backend**: Automatic restart on source changes via sbt-revolver
- **Frontend**: Vite dev server with Hot Module Replacement
- **Scala.js**: Incremental compilation with fast linking

## 🧪 Testing Strategy

### **Test Pyramid**
```bash
# Unit Tests - Fast, isolated business logic tests
sbt 'backend/test'

# Integration Tests - Database and service integration  
# Uses TestContainers for PostgreSQL and Redis

# E2E Tests - Full browser automation with Playwright
# Tests complete user workflows across the application
```

### **Test Features**
- **Weaver Test Framework**: Cats Effect-based testing with excellent async support
- **TestContainers**: Isolated database instances for integration tests
- **Playwright Integration**: Modern browser automation for E2E scenarios
- **Property-Based Testing**: ScalaCheck integration for robust testing

## 🚢 Deployment

### **Kubernetes Deployment**
```bash
cd infra/

# Deploy to development
./deploy.sh dev

# Deploy to production  
./deploy.sh prod

# Validate deployment
./validate-deployment.sh
```

### **Infrastructure Components**
- **Application Pods**: Horizontally scalable backend instances
- **PostgreSQL**: Persistent database with backup strategies
- **Redis**: High-performance caching layer
- **Ingress**: Load balancing and TLS termination
- **Monitoring**: Observability stack with metrics and logging

### **Docker Images**
- **Base Image**: `wonder/jdk:17.0.10_7-ubuntu`
- **Registry**: GitHub Container Registry (`ghcr.io`)
- **Multi-stage Builds**: Optimized for production deployment

## 📁 Project Structure

```
real-world-scala3/
├── modules/
│   ├── shared/          # Cross-platform code
│   │   └── smithy/      # API specifications
│   ├── backend/         # JVM server
│   │   ├── domain/      # Business entities
│   │   ├── http/        # HTTP endpoints
│   │   ├── repo/        # Database access
│   │   └── service/     # Business logic
│   ├── frontend/        # Scala.js client
│   │   └── src/main/scala/
│   └── app/            # Application assembly
├── infra/              # Infrastructure as Code
│   ├── yamls/          # Kubernetes manifests
│   └── pulumi/         # Pulumi configuration
└── justfile           # Development commands
```

## 🤝 Contributing

This project follows **functional programming best practices** and **type-driven development**:

1. **Fork** the repository
2. **Create** a feature branch
3. **Write tests** first (TDD approach)
4. **Implement** with types leading the design
5. **Ensure** all tests pass: `just test`
6. **Submit** a pull request

### **Code Style**
- **Scalafmt**: Automatic formatting (max width: 120)
- **Scalafix**: Linting and refactoring rules
- **Unused Code**: Warnings enabled (`-Wunused:all`)

## 📖 Learn More

### **Key Concepts Demonstrated**
- **Opaque Types**: Type-safe domain modeling without runtime overhead
- **Refined Types**: Compile-time validation with Iron
- **Effect Systems**: Pure functional programming with Cats Effect
- **Cross-Platform Development**: Shared code between JVM and JavaScript
- **Contract-First APIs**: Type-safe client/server with Smithy4s

### **Resources**
- [Smithy4s Documentation](https://smithy4s.dev)
- [Laminar Guide](https://laminar.dev/documentation)
- [Http4s Tutorial](https://http4s.org/v0.23/docs/)
- [Cats Effect Documentation](https://typelevel.org/cats-effect/)

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ and Scala 3** - Showcasing the future of functional programming