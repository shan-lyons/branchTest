package com.akqa.audi.userprofile.web.controller;

import com.akqa.audi.user.domain.Email;
import com.akqa.audi.user.domain.Name;
import com.akqa.audi.user.domain.Profile;
import com.akqa.audi.user.domain.Token;
import com.akqa.audi.user.service.ProfileException;
import com.akqa.audi.user.service.ProfileManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Shan Lyons
 */
@Controller
@RequestMapping("/user")
public class UserProfileRestController {


//    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileRestApi.class);
    private static final String FIRST_NAME = "first_name";
    private static final String LAST_NAME = "last_name";
    private static final String PASSWORD_PARAM = "password";
    private static final String EMAIL_PARAM = "email";

//    @Autowired
//    private String serviceProviderHost;

//    @Autowired
//    private ProfileCredentialsManagementService credentialsManagementService;

    @Autowired
    private ProfileManagementService profileManagementService;

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @ResponseBody
    public Token register(final HttpServletRequest request, final HttpServletResponse response) throws ProfileException {
        // TODO: utilize spring binding for this.
//    public String register(
//            @Valid @ModelAttribute(RegistrationBean.COMPONENT_NAME) final RegistrationBean form,
//            final Errors bindingErrors,
//            final Model model
//    ) throws ProfileException {
        final String firstName = request.getParameter(FIRST_NAME);
        final String lastName = request.getParameter(LAST_NAME);
        final String email = request.getParameter(EMAIL_PARAM);
        final String password = request.getParameter(PASSWORD_PARAM);

        final Name name = new Name.Builder().firstName(firstName).lastName(lastName).build();
        final Email theEmail = new Email.Builder().email(email).build();
        final Profile profile = new Profile.Builder().name(name).email(theEmail).build();

        return profileManagementService.register(profile, password, false);
    }

    @RequestMapping("/profile")
    @ResponseBody
    public Profile getProfile(final HttpServletRequest request, final HttpServletResponse response) throws ProfileException {
        final String email = request.getParameter(EMAIL_PARAM);

        return profileManagementService.getProfile(new Email.Builder().email(email).build());
    }

}
