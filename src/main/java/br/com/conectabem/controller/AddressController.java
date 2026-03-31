package br.com.conectabem.controller;

import br.com.conectabem.dto.address.AddressCreationDTO;
import br.com.conectabem.dto.address.AddressListRequest;
import br.com.conectabem.dto.address.AddressListResponse;
import br.com.conectabem.model.Address;
import br.com.conectabem.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<AddressListResponse> list(@ModelAttribute AddressListRequest request) {
        return ResponseEntity.ok(addressService.list(request));
    }

    @PostMapping
    public ResponseEntity<Address> createAddress(@RequestBody AddressCreationDTO request) {
        Address address = addressService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }
}
