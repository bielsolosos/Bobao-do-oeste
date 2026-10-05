package br.dev.bielsolosos.biscraper.api.controller.user;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User Profile & Config", description = "Endpoints de perfil e configurações do usuário autenticado")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor 
public class UserController {
    
    

}
