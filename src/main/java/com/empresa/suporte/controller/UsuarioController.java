package com.empresa.suporte.controller;

import com.empresa.suporte.config.SecurityWebConfig;
import com.empresa.suporte.img.FileUploadUtil;
import com.empresa.suporte.model.Usuario;
import com.empresa.suporte.repository.SalaRepository;
import com.empresa.suporte.repository.PermissaoRepository;
import com.empresa.suporte.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

@Controller
public class UsuarioController {

    public boolean erroLogin = false;
    public boolean erroCpf = false;
    public boolean erroEmail = false;
    public String uploadDir;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    PermissaoRepository permissaoRepository;

    @Autowired
    private SalaRepository salaRepository;

    @GetMapping("/usuario/list")
    public String listUsuario(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll(Sort.by("nome")));
        return "usuario/list";
    }

    @GetMapping("/usuario/add")
    public String addUsuario(Model model) {

        model.addAttribute("usuario", new Usuario());
        model.addAttribute("salaSegunda", salaRepository.findBySemanaAndLimite("Segunda-feira"));
        model.addAttribute("salaTerca", salaRepository.findBySemanaAndLimite("Terça-feira"));
        model.addAttribute("salaQuarta", salaRepository.findBySemanaAndLimite("Quarta-feira"));
        model.addAttribute("salaQuinta", salaRepository.findBySemanaAndLimite("Quinta-feira"));
        model.addAttribute("salaSexta", salaRepository.findBySemanaAndLimite("Sexta-feira"));
        model.addAttribute("salaSabado", salaRepository.findBySemanaAndLimite("Sábado"));

        if (erroLogin || erroCpf || erroEmail) {

            if (erroCpf)
                model.addAttribute("erroCpf", "true");
            if (erroEmail)
                model.addAttribute("erroEmail", "true");
            if (erroLogin)
                model.addAttribute("erroLogin", "true");

            erroLogin = false;
            erroCpf = false;
            erroEmail = false;
        }

        return ("usuario/add");
    }

    @PostMapping("/usuario/save")
    public RedirectView saveUsuario(Usuario usuario, Model model, @RequestParam("image") MultipartFile imagem)
            throws IOException {
        try {
            if (usuario != null) {
                boolean erro = false;
                SecurityWebConfig.geraSenha(usuario);
                if (usuario.getId() == null) {
                    String url = "/usuario/add";
                    model.addAttribute("usuario", usuario);
                    model.addAttribute("salaSegunda", salaRepository.findBySemanaAndLimite("Segunda-feira"));
                    model.addAttribute("salaTerca", salaRepository.findBySemanaAndLimite("Terça-feira"));
                    model.addAttribute("salaQuarta", salaRepository.findBySemanaAndLimite("Quarta-feira"));
                    model.addAttribute("salaQuinta", salaRepository.findBySemanaAndLimite("Quinta-feira"));
                    model.addAttribute("salaSexta", salaRepository.findBySemanaAndLimite("Sexta-feira"));
                    model.addAttribute("salaSabado", salaRepository.findBySemanaAndLimite("Sábado"));

                    if (usuarioRepository.findByLogin(usuario.getLogin()) != null) {
                        erroLogin = true;
                    }

                    if (usuarioRepository.findByCpf(usuario.getCpf()) != null) {
                        erroCpf = true;
                    }

                    if (usuarioRepository.findByEmail(usuario.getEmail()) != null) {
                        erroEmail = true;
                    }

                    if (erroLogin == true || erroCpf == true || erroEmail == true) {
                        return new RedirectView("/usuario/add");
                    }

                    if (imagem != null && !imagem.isEmpty()) {
                        // Salvar a imagem diretamente no objeto de usuário como um campo byte[]
                        usuario.setFoto(imagem.getBytes());
                    }

                    Usuario savedUsuario = usuarioRepository.save(usuario);
                    url = "redirect:/usuario/view/" + savedUsuario.getId() + "/" + true;
                } else {
                    String url = "/usuario/edit";
                    model.addAttribute("usuario", usuario);
                    model.addAttribute("salaSegunda", salaRepository.findBySemanaAndLimite("Segunda-feira"));
                    model.addAttribute("salaTerca", salaRepository.findBySemanaAndLimite("Terça-feira"));
                    model.addAttribute("salaQuarta", salaRepository.findBySemanaAndLimite("Quarta-feira"));
                    model.addAttribute("salaQuinta", salaRepository.findBySemanaAndLimite("Quinta-feira"));
                    model.addAttribute("salaSexta", salaRepository.findBySemanaAndLimite("Sexta-feira"));
                    model.addAttribute("salaSabado", salaRepository.findBySemanaAndLimite("Sábado"));

                    if (usuarioRepository.findByLoginAndIdNot(usuario.getLogin(), usuario.getId()) != null) {
                        erroLogin = true;
                    }

                    if (usuarioRepository.findByEmailAndIdNot(usuario.getEmail(), usuario.getId()) != null) {
                        erroEmail = true;
                    }

                    if (erroLogin == true || erroEmail == true) {
                        return new RedirectView("/usuario/edit/" + usuario.getId());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao salvar: " + e.getMessage());
            // Lógica de tratamento de erro, se necessário...
        }

        return new RedirectView("/usuario/list", true);
    }

    @GetMapping("/usuario/view/{id}/{salvo}")
    public String viewUsuario(@PathVariable long id, @PathVariable boolean salvo, Model model) {

        Usuario usuario = usuarioRepository.findById(id).get();

        model.addAttribute("usuario", usuario);
        model.addAttribute("salvo", salvo);

        if (usuario.getFoto().equals("")) {
            model.addAttribute("nullFoto", true);
        } else {
            model.addAttribute("nullFoto", false);
        }

        return "usuario/view_modal";
    }

    // ----------VIEW USER----------
    @GetMapping("/usuario/viewUser")
    public String viewUser(Model model) {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByLogin(login);
        model.addAttribute("usuario", usuario);

        if (usuario.getFoto() != null && usuario.getFoto().length > 0) {
            String fotoBase64 = Base64.getEncoder().encodeToString(usuario.getFoto());
            model.addAttribute("fotoBase64", fotoBase64);
        }

        return "usuario/viewUser";
    }

    @GetMapping("/usuario/edit/{id}")
    public String editUsuario(@PathVariable long id, Model model) {

        model.addAttribute("salaSegunda", salaRepository.findBySemanaAndLimite("Segunda-feira", id));
        model.addAttribute("salaTerca", salaRepository.findBySemanaAndLimite("Terça-feira", id));
        model.addAttribute("salaQuarta", salaRepository.findBySemanaAndLimite("Quarta-feira", id));
        model.addAttribute("salaQuinta", salaRepository.findBySemanaAndLimite("Quinta-feira", id));
        model.addAttribute("salaSexta", salaRepository.findBySemanaAndLimite("Sexta-feira", id));
        model.addAttribute("salaSabado", salaRepository.findBySemanaAndLimite("Sábado", id));

        if (erroLogin == true || erroEmail == true) {

            if (erroLogin == true)
                model.addAttribute("erroLogin", "true");
            if (erroEmail == true)
                model.addAttribute("erroEmail", "true");
            erroLogin = false;
            erroEmail = false;
        }

        Usuario usuario = usuarioRepository.findById(id).get();

        if (usuario.getFoto().equals("")) {
            model.addAttribute("nullFoto", true);
        } else {
            model.addAttribute("nullFoto", false);
        }

        model.addAttribute("usuario", usuarioRepository.findById(id));

        return "usuario/edit";

    }

    @GetMapping("/usuario/delete/{id}")
    public String deleteUsuario(@PathVariable long id) {
        try {
            usuarioRepository.deleteById(id);

        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
        return "redirect:/usuario/list";
    }

    @GetMapping("/usuario/imagem/{id}")
    public ResponseEntity<byte[]> getUsuarioImagem(@PathVariable long id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario != null && usuario.getFoto() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);

            return new ResponseEntity<>(usuario.getFoto(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
