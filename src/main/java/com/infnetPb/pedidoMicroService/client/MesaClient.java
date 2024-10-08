package com.infnetPb.pedidoMicroService.client;

import com.infnetPb.pedidoMicroService.DTO.MesaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "mesa-service", url = "http://mesa-service:8082")
public interface MesaClient {

    @GetMapping("/mesas/{id}")
    MesaDTO getMesaById(@PathVariable("id") UUID id);

    @PutMapping("/mesas/{id}")
    MesaDTO updateMesa(@PathVariable UUID id, @RequestBody MesaDTO mesaDTO);
}