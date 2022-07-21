package org.mitre.healthmanager.fhir.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Forwards hard-coded tester overlay paths to /tester/*.
 * Will override other mappings.
 */
@Controller
@ConditionalOnExpression("'${hapi.fhir.tester}' != null")
public class TesterOverlayForwardingController {
    @GetMapping("/home")
    public String forwardHome() {
        return "redirect:/tester/home";
    }
    @GetMapping("/about")
    public String forwardAbout() {
        return "redirect:/tester/about";
    }
    
    @GetMapping("/resource")
    public String forwardResource() {
        return "redirect:/tester/resource";
    }
    @GetMapping("/search")
    public String forwardSearch() {
        return "redirect:/tester/search";
    }
    @GetMapping("/read")
    public String forwardRead() {
        return "redirect:/tester/read";
    }
    @GetMapping("/history-type")
    public String forwardHistoryType() {
        return "redirect:/tester/history-type";
    }
    @GetMapping("/delete")
    public String forwardDelete() {
        return "redirect:/tester/delete";
    }
    @PostMapping("/create")
    public String forwardCreate() {
        return "redirect:/tester/create";
    }
    @PostMapping("/update")
    public String forwardUpdate() {
        return "redirect:/tester/update";
    }
    @PostMapping("/validate")
    public String forwardValidate() {
    	return "redirect:/tester/validate";
    }
}
