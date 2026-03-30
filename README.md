# Upchain

A Kotlin Multiplatform library for building append-only, cryptographically linked data structures with built-in synchronization capabilities.

## Overview

Upchain is an immutable, append-only chain of updates where each link is cryptographically hashed to the previous one. This creates a tamper-evident data structure similar to a blockchain but designed for local data synchronization between devices.

### Key Features

- **Immutable Append-Only Chain**: Once added, updates cannot be modified or removed
- **Cryptographic Linking**: Each update is hashed with the previous one using SHA-256
- **Content Deduplication**: Duplicate updates are automatically detected and ignored
- **Kotlin Multiplatform**: Supports JVM and Linux X64 targets
- **Coroutines-Based**: Built on Kotlin Coroutines with StateFlow for reactive updates
- **Pluggable Storage**: File-based storage included, with support for custom mediators

## How It Works

### Core Concepts

#### Upchain

An `Upchain` is an immutable chain of `Update` items. Each item contains:
- `update`: The actual data (a string value)
- `hash`: A cryptographic hash linking this item to the previous one

```kotlin
val upchain = Upchain.empty
    .plus(Update("First update"))
    .plus(Update("Second update"))
```

#### Hash Chain

Each update's hash is computed as:
```
hash[n] = SHA256(hash[n-1] + update[n])
```

This creates a chain where any modification to historical data would break all subsequent hashes, making tampering detectable.

#### Content Deduplication

Updates with identical content are automatically deduplicated using content hashing. Adding the same update twice will not create a duplicate entry.

### Synchronization Logic

Upchain uses a repository pattern with a mediator for persistence:

```
┌─────────────────┐
│  UpchainRepository │
│  (in-memory)    │
└────────┬────────┘
         │ StateFlow<Upchain>
         │
         ▼
┌─────────────────┐
│  UpchainMediator │
│  (persistence)  │
└─────────────────┘
```

#### UpchainRepository

The `UpchainRepository` provides:
- `upchain: StateFlow<Upchain>` - Reactive access to the current chain state
- `editWithResult()` - Thread-safe modifications with mutex locking

#### UpchainMediator

The mediator handles persistence operations:
- `append(updates)` - Append new updates to storage (efficient for logs)
- `replace(updates)` - Replace entire chain (used on divergence)

When the chain diverges (e.g., a conflicting update is applied), the mediator uses `replace()` to rewrite the entire storage. Otherwise, it uses `append()` for efficiency.

#### Conflict Resolution

Upchain detects divergence by comparing hashes:
- If the new chain's hash is in the current chain's history → append new updates
- If not → replace the entire chain (divergence detected)

```kotlin
// Divergence example
val current = Upchain.empty + Update("A") + Update("B")
val diverged = Upchain.empty + Update("A") + Update("C")  // Different branch
```

### UpchainsRepository

Manages multiple upchains by ID:
- `createUpchain(id)` - Create a new upchain
- `upchains: StateFlow<List<Item>>` - Reactive list of all upchains
- Each item provides `id`, `repository`, and `remove()` function

## Usage

### Basic Usage

```kotlin
import org.hnau.upchain.core.*
import org.hnau.upchain.core.repository.upchain.*

// Create an empty upchain
var upchain = Upchain.empty

// Add updates
upchain = upchain + Update("Hello")
upchain = upchain + Update("World")

// Access items
println(upchain.items.size)  // 2
println(upchain.peekHash)    // Latest hash
```

### Repository Usage

```kotlin
import org.hnau.upchain.core.repository.upchain.*
import kotlinx.coroutines.flow.collectLatest

// Create a repository with in-memory mediator
val repository = UpchainRepository.create(
    updates = emptyList(),
    mediator = object : UpchainMediator {
        override suspend fun append(updates: NonEmptyList<Update>) {
            // Persist to your storage
        }
        
        override suspend fun replace(updates: List<Update>) {
            // Replace entire chain
        }
    }
)

// Observe changes
repository.upchain.collectLatest { upchain ->
    println("Chain has ${upchain.items.size} items")
}

// Add updates
repository.addUpdate(Update("New data"))

// Or add multiple
repository.addUpdates(listOf(
    Update("Update 1"),
    Update("Update 2")
))
```

### File-Based Storage

```kotlin
import org.hnau.upchain.core.repository.file.upchain.fileBased

// Create a file-backed repository
val repository = UpchainRepository.fileBased(
    filename = "/path/to/chain.txt"
)

// All updates are automatically persisted to the file
repository.addUpdate(Update("Persisted update"))
```

### Multiple Upchains

```kotlin
import org.hnau.upchain.core.repository.file.upchains.fileBased
import org.hnau.upchain.core.UpchainId
import kotlin.uuid.Uuid

// Create a repository managing multiple upchains in a directory
val upchainsRepo = UpchainsRepository.fileBased(
    dir = "/path/to/chains/"
)

// Create a new upchain
val newId = UpchainId.createRandom()
upchainsRepo.createUpchain(newId)

// Access all upchains
upchainsRepo.upchains.collectLatest { items ->
    items.forEach { item ->
        println("Upchain ${item.id}: ${item.repository.upchain.value.items.size} items")
    }
}

// Remove an upchain
val item = upchainsRepo.upchains.value.first()
item.remove()
```

### Custom Mediator Example

```kotlin
// Network synchronization mediator
class NetworkMediator(
    private val client: HttpClient,
    private val endpoint: String
) : UpchainMediator {
    
    override suspend fun append(updates: NonEmptyList<Update>) {
        client.post("$endpoint/append") {
            setBody(updates.map { it.value })
        }
    }
    
    override suspend fun replace(updates: List<Update>) {
        client.post("$endpoint/replace") {
            setBody(updates.map { it.value })
        }
    }
}
```

## Architecture

```
org.hnau.upchain.core
├── Upchain              # Immutable chain data structure
├── Update               # Value class for update content
├── UpchainHash          # Cryptographic hash of chain position
├── UpchainId            # UUID-based identifier for upchains
└── utils
    ├── ContentHash      # Content-based deduplication hash
    └── SHA256           # Platform-specific hashing

org.hnau.upchain.core.repository
├── upchain
│   ├── UpchainRepository      # Repository interface
│   ├── UpchainMediator        # Persistence mediator
│   └── FileBasedUpchainRepository  # File implementation
└── upchains
    ├── UpchainsRepository     # Multi-chain repository
    └── FileBasedUpchainsRepository # File implementation
```

## Dependencies

- Kotlin 2.3.10
- Kotlinx Coroutines 1.10.2
- Kotlinx Serialization 1.10.0
- Arrow Core 2.2.2 (for NonEmptyList)
- Ktor (optional, for networking)

## License

MIT License
