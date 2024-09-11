package com.infnetPb.pedidoMicroService.service;

import com.infnetPb.pedidoMicroService.DTO.MesaDTO;
import com.infnetPb.pedidoMicroService.DTO.PedidoDTO;
import com.infnetPb.pedidoMicroService.DTO.RestauranteDTO;
import com.infnetPb.pedidoMicroService.client.MesaClient;
import com.infnetPb.pedidoMicroService.client.RestauranteClient;
import com.infnetPb.pedidoMicroService.event.PedidoCriadoEvent;
import com.infnetPb.pedidoMicroService.model.Pedido;
import com.infnetPb.pedidoMicroService.model.history.PedidoHistory;
import com.infnetPb.pedidoMicroService.repository.PedidoRepository;
import com.infnetPb.pedidoMicroService.repository.history.PedidoHistoryRepository;
import jakarta.transaction.Transactional;
import org.apache.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RestauranteClient restauranteClient;
    @Autowired
    private MesaClient mesaClient;

    @Autowired
    private PedidoHistoryRepository pedidoHistoryRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final static Logger logger = Logger.getLogger(PedidoService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    @Transactional
    public PedidoDTO createPedido(PedidoDTO pedidoDTO) {
        RestauranteDTO restaurante = restauranteClient.getRestauranteById(pedidoDTO.getRestauranteId());
        if (restaurante == null) {
            logger.error("Restaurante não encontrado");
            throw new RuntimeException("Restaurante não encontrado");
        }

        MesaDTO mesa = mesaClient.getMesaById(pedidoDTO.getMesaId());
        if (mesa == null) {
            logger.error("Mesa não encontrada");
            throw new RuntimeException("Mesa não encontrada");
        }

        if (mesa.getPedidos() != null && !mesa.getPedidos().isEmpty()) {
            logger.error("A Mesa já tem um pedido criado.");
            throw new RuntimeException("A Mesa já tem um pedido criado.");
        }

        Pedido pedido = new Pedido();
        pedido.setDescricaoPedido(pedidoDTO.getDescricaoPedido());
        pedido.setValorTotal(pedidoDTO.getValorTotal());
        pedido.setRestauranteId(pedidoDTO.getRestauranteId());
        pedido.setMesaId(pedidoDTO.getMesaId());

        Pedido savedPedido = pedidoRepository.save(pedido);
        savePedidoHistory(savedPedido, "CREATE");

        mesa.getPedidos().add(savedPedido.getId().toString());
        mesaClient.updateMesa(mesa.getId(), mesa);

        PedidoCriadoEvent event = new PedidoCriadoEvent(
                savedPedido.getId(),
                savedPedido.getDescricaoPedido(),
                savedPedido.getValorTotal(),
                savedPedido.getMesaId(),
                savedPedido.getRestauranteId()
        );

        logger.info("Tentando enviar evento PedidoCriado: " + event);

        CompletableFuture.runAsync(() -> {
            boolean success = sendEventWithRetry(event, "pedidoExchange", "pedidoCriado");
            if (success) {
                logger.info("Evento PedidoCriado enviado com sucesso.");
            } else {
                logger.error("Falha ao enviar evento PedidoCriado após " + MAX_RETRIES + " tentativas.");
            }
        });

        return mapToDTO(savedPedido);
    }

    private boolean sendEventWithRetry(Object event, String exchange, String routingKey) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, event);
                return true;
            } catch (Exception e) {
                logger.error("Erro ao enviar evento (tentativa " + attempt + "): " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return false;
    }

    public PedidoDTO updatePedido(UUID id, PedidoDTO pedidoDTO) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setDescricaoPedido(pedidoDTO.getDescricaoPedido());
        pedido.setValorTotal(pedidoDTO.getValorTotal());

        Pedido updatedPedido = pedidoRepository.save(pedido);

        savePedidoHistory(updatedPedido, "UPDATE");

        return mapToDTO(updatedPedido);
    }

    public void deletePedidoById(UUID id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedidoRepository.deleteById(id);
        savePedidoHistory(pedido, "DELETE");
    }

    public List<PedidoDTO> getAllPedidos() {
        List<Pedido> pedidos = pedidoRepository.findAll();
        return pedidos.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public PedidoDTO getPedidoById(UUID id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        return mapToDTO(pedido);
    }

    public List<PedidoHistory> getAllPedidoHistories() {
        return pedidoHistoryRepository.findAll();
    }

    private void savePedidoHistory(Pedido pedido, String operation) {
        PedidoHistory pedidoHistory = new PedidoHistory();
        pedidoHistory.setPedido(pedido);
        pedidoHistory.setDescricaoPedido(pedido.getDescricaoPedido());
        pedidoHistory.setValorTotal(pedido.getValorTotal());
        pedidoHistory.setTimestamp(LocalDateTime.now());
        pedidoHistory.setOperation(operation);
        pedidoHistoryRepository.save(pedidoHistory);
    }


    private PedidoDTO mapToDTO(Pedido pedido) {
        PedidoDTO pedidoDTO = new PedidoDTO();
        pedidoDTO.setId(pedido.getId());
        pedidoDTO.setDescricaoPedido(pedido.getDescricaoPedido());
        pedidoDTO.setValorTotal(pedido.getValorTotal());
        pedidoDTO.setRestauranteId(pedido.getRestauranteId());
        pedidoDTO.setMesaId(pedido.getMesaId());
        return pedidoDTO;
    }
}