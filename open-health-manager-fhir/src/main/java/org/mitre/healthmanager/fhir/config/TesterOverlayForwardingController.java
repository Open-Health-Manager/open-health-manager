package org.mitre.healthmanager.fhir.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Forwards hard-coded tester overlay paths to /tester/*.
 * Will override other mappings.
 */
@Controller
public class TesterOverlayForwardingController {
    @GetMapping("/home")
    public String forwardHome() {
        return "forward:/tester/home";
    }
    @GetMapping("/about")
    public String forwardAbout() {
        return "forward:/tester/about";
    }
    
    @GetMapping("/resource")
    public String forwardResource() {
        return "forward:/tester/resource";
    }
    @GetMapping("/search")
    public String forwardSearch() {
        return "forward:/tester/search";
    }
    @GetMapping("/read")
    public String forwardRead() {
        return "forward:/tester/read";
    }
    @GetMapping("/history-type")
    public String forwardHistoryType() {
        return "forward:/tester/history-type";
    }
    @GetMapping("/delete")
    public String forwardDelete() {
        return "forward:/tester/delete";
    }
    @PostMapping("/create")
    public String forwardCreate() {
        return "forward:/tester/create";
    }
    @PostMapping("/update")
    public String forwardUpdate() {
        return "forward:/tester/update";
    }
    @PostMapping("/validate")
    public String forwardValidate() {
        return "forward:/tester/validate";
    }
    
    @GetMapping(path = {"/css/{res}"})
    public String forwardCss(@PathVariable String res) {
        return "forward:/tester/css/" + res;
    }

    @GetMapping(path = {"/img/{res}"})
    public String forwardImg(@PathVariable String res) {
        return "forward:/tester/img/" + res;
    }
    
    @GetMapping(path = {"/js/{res}"})
    public String forwardJs(@PathVariable String res) {
        return "forward:/tester/js/" + res;
    }
    
    @GetMapping(path = {"/fa/{res}"})
    public String forwardFa(@PathVariable String res) {
        return "forward:/tester/fa/" + res;
    }
    
    @GetMapping(path = {"/fonts/{res}"})
    public String forwardFonts(@PathVariable String res) {
        return "forward:/tester/fonts/" + res;
    }
}
