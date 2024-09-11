package com.infnetPb.pedidoMicroService.controller;


import com.infnetPb.pedidoMicroService.DTO.PedidoDTO;
import com.infnetPb.pedidoMicroService.model.history.PedidoHistory;
import com.infnetPb.pedidoMicroService.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {


    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoDTO> createPedido(@RequestBody PedidoDTO pedidoDTO) {
        try {
            PedidoDTO createdPedido = pedidoService.createPedido(pedidoDTO);
            return new ResponseEntity<>(createdPedido, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> getPedidoById(@PathVariable UUID id) {
        PedidoDTO pedidoDTO = pedidoService.getPedidoById(id);
        return ResponseEntity.ok(pedidoDTO);
    }

    @GetMapping
    public ResponseEntity<List<PedidoDTO>> getAllPedidos() {
        List<PedidoDTO> pedidos = pedidoService.getAllPedidos();
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePedidoById(@PathVariable UUID id) {
        try {
            pedidoService.deletePedidoById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoDTO> updatePedido(@PathVariable UUID id, @RequestBody PedidoDTO pedidoDTO) {
        try {
            PedidoDTO updatedPedido = pedidoService.updatePedido(id, pedidoDTO);
            return new ResponseEntity<>(updatedPedido, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/historico")
    public ResponseEntity<List<PedidoHistory>> getAllPedidoHistories() {
        List<PedidoHistory> history = pedidoService.getAllPedidoHistories();
        return new ResponseEntity<>(history, HttpStatus.OK);
    }
}