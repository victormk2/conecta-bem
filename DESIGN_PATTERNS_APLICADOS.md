# Padroes de Projeto Aplicados

Este documento descreve os 3 padroes aplicados no projeto, com trechos reais para demonstrar como foram usados.

## 1) Factory Method - Criacao de Usuario no cadastro

### Conceito
Factory Method centraliza a criacao de objetos em uma fabrica, removendo essa responsabilidade de servicos que devem focar em regra de negocio.

### Onde foi aplicado
- Interface: `src/main/java/br/com/conectabem/service/auth/factory/UserFactory.java`
- Implementacao: `src/main/java/br/com/conectabem/service/auth/factory/DefaultUserFactory.java`
- Uso no fluxo: `src/main/java/br/com/conectabem/service/AuthService.java` (metodo `register`)

### Trecho de codigo (declaracao)
```java
public interface UserFactory {
    User create(RegisterRequest request);
}
```

```java
@Component
public class DefaultUserFactory implements UserFactory {
    @Override
    public User create(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setRole(UserRole.USER);
        user.setPassword(passwordEncoder.encode(request.password()));
        return user;
    }
}
```

### Trecho de codigo (uso no servico)
```java
public String register(RegisterRequest request) {
    User user = userFactory.create(request);
    repository.save(user);
    return jwtService.generateToken(user.getUsername());
}
```

### Beneficio
- `AuthService` fica menor e mais coeso
- regras de criacao ficam em um unico lugar
- facilita reutilizacao e teste de criacao de usuario

---

## 2) Strategy - Busca de usuario por identificador

### Conceito
Strategy encapsula variacoes de comportamento em classes diferentes, escolhidas em tempo de execucao.

### Onde foi aplicado
- Contrato: `src/main/java/br/com/conectabem/service/auth/strategy/UserLookupStrategy.java`
- Estrategia por e-mail: `src/main/java/br/com/conectabem/service/auth/strategy/EmailUserLookupStrategy.java`
- Estrategia por username: `src/main/java/br/com/conectabem/service/auth/strategy/UsernameUserLookupStrategy.java`
- Uso no fluxo: `src/main/java/br/com/conectabem/service/AuthService.java` (metodos `login`, `getUserId`, `checkAccess`)

### Trecho de codigo (contrato + estrategias)
```java
public interface UserLookupStrategy {
    boolean supports(String identifier);
    Optional<User> find(String identifier);
}
```

```java
@Component
@Order(1)
public class EmailUserLookupStrategy implements UserLookupStrategy {
    @Override
    public boolean supports(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    @Override
    public Optional<User> find(String identifier) {
        return repository.findByEmail(identifier);
    }
}
```

```java
@Component
@Order(2)
public class UsernameUserLookupStrategy implements UserLookupStrategy {
    @Override
    public boolean supports(String identifier) {
        return identifier != null && !identifier.contains("@");
    }

    @Override
    public Optional<User> find(String identifier) {
        return repository.findByUsername(identifier);
    }
}
```

### Trecho de codigo (uso no servico)
```java
private Optional<User> resolveByIdentifier(String identifier) {
    return userLookupStrategies.stream()
            .filter(strategy -> strategy.supports(identifier))
            .findFirst()
            .flatMap(strategy -> strategy.find(identifier));
}

public String login(LoginRequest request) {
    User user = resolveByIdentifier(request.username()).orElseThrow();
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
        throw new RuntimeException("Invalid credentials");
    }
    return jwtService.generateToken(user.getUsername());
}
```

### Beneficio
- remove condicoes espalhadas para escolher busca
- torna facil adicionar novos tipos de identificador (ex.: telefone)
- reduz acoplamento do servico ao detalhe de consulta

---

## 3) Facade - Processamento de imagem de evento

### Conceito
Facade oferece uma interface simples para uma logica que pode ter varias validacoes e passos tecnicos.

### Onde foi aplicado
- Contrato: `src/main/java/br/com/conectabem/service/image/ImageProcessingFacade.java`
- Implementacao: `src/main/java/br/com/conectabem/service/image/DefaultImageProcessingFacade.java`
- Uso no fluxo: `src/main/java/br/com/conectabem/service/impl/EventServiceImpl.java`

### Trecho de codigo (declaracao)
```java
public interface ImageProcessingFacade {
    void applyIfPresent(Event event, MultipartFile image);
}
```

```java
@Component
public class DefaultImageProcessingFacade implements ImageProcessingFacade {
    @Override
    public void applyIfPresent(Event event, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }
        if (image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Uploaded file must be an image.");
        }
        try {
            event.setImage(image.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not process uploaded image.");
        }
    }
}
```

### Trecho de codigo (uso no servico)
```java
public Event createWithImage(EventCreationDTO eventCreationDTO, MultipartFile image) {
    var baseEntity = creationToEntity.map(eventCreationDTO);
    baseEntity.setOwner(userService.findById(currentUserService.requireUserId()));
    baseEntity.setAddress(addressService.findById(UUID.fromString(eventCreationDTO.addressId())));
    imageProcessingFacade.applyIfPresent(baseEntity, image);
    return eventRepository.save(baseEntity);
}
```

### Beneficio
- `EventServiceImpl` fica focado em regra de negocio de evento
- processamento de arquivo fica reutilizavel e testavel isoladamente
- melhora legibilidade e manutencao

---

## Resumo de impacto arquitetural

Com os 3 padroes, a estrutura ficou mais modular:
- criacao de entidade (Factory)
- variacao de busca/autenticacao (Strategy)
- operacao tecnica de arquivo (Facade)

Isso reduz acoplamento, melhora coesao e facilita evolucao do projeto para novos requisitos.

