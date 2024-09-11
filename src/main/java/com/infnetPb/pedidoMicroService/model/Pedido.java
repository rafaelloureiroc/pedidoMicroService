package com.infnetPb.pedidoMicroService.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class Pedido {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    private String descricaoPedido;
    private double valorTotal;

    private UUID mesaId;
    private UUID restauranteId;

}