package org.mitre.healthmanager.fhir.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Redirects or forwards hard-coded tester overlay links to /tester/*.
 * Will override other mappings.
 */
@Controller
@ConditionalOnExpression("'${hapi.fhir.tester}' != null")
public class TesterOverlayForwardingController {
    @GetMapping({"/home", "/about", "/resource", "/search", "/read", "/history-type", "/delete"})
    public String redirectGetRequest(HttpServletRequest request) {    	
        StringBuilder requestURL = new StringBuilder("redirect:/tester");
        String path = new UrlPathHelper().getPathWithinApplication(request);
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.append(path).toString();
        } else {
            return requestURL.append(path).append('?').append(queryString).toString();
        }
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
