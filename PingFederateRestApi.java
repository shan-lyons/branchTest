package com.akqa.audi.userprofile.web.controller;

import com.akqa.audi.user.domain.Car;
import com.akqa.audi.user.domain.DealerId;
import com.akqa.audi.user.domain.Email;
import com.akqa.audi.user.domain.Location;
import com.akqa.audi.user.domain.Model;
import com.akqa.audi.user.domain.Name;
import com.akqa.audi.user.domain.Phone;
import com.akqa.audi.user.domain.Preferences;
import com.akqa.audi.user.domain.Profile;
import com.akqa.audi.user.domain.Subject;
import com.akqa.audi.user.domain.Subscription;
import com.akqa.audi.user.service.LoginResponse;
import com.akqa.audi.user.service.ProfileCredentialsManagementService;
import com.akqa.audi.user.service.ProfileException;
import com.akqa.audi.user.service.ProfileManagementService;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/pingFederate")
@SuppressWarnings("PMD.ExcessiveImports")
public class PingFederateRestApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(PingFederateRestApi.class);
    private static final String VIN_PARAM = "VIN";
    private static final String USERNAME_PARAM = "username";
    private static final String PASSWORD_PARAM = "password";
    private static final String EMAIL_PARAM = "email";

    @Autowired
    private String serviceProviderHost;

    @Autowired
    private ProfileCredentialsManagementService credentialsManagementService;

    @Autowired
    private ProfileManagementService profileManagementService;


    @RequestMapping("/register")
    public String registerUser(final HttpServletRequest request, final HttpServletResponse response) throws ProfileException {
        final String email = request.getParameter(EMAIL_PARAM);
        final String password = request.getParameter(PASSWORD_PARAM);

        // TODO: ensure that email is not already in use
        profileManagementService.register(createSilverProfile(email), password, false);

        return "register";
    }

    public com.akqa.audi.user.domain.Profile createSilverProfile(final String email) {
        return new com.akqa.audi.user.domain.Profile.Builder().name(createDomainName())
                .email(createDomainEmail(email)).build();
    }


    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public String authenticate(final HttpServletRequest request, final HttpServletResponse response) {
        getLogger().trace("authenticate");
        String result = "validUser";
        final String username = request.getParameter(USERNAME_PARAM);
        final String password = request.getParameter(PASSWORD_PARAM);
        final LoginResponse loginResponse = credentialsManagementService.validateUserCredentials(username, password);
        if (!LoginResponse.SUCCESS.equals(loginResponse)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            result = "invalid";
        }
        return result;
    }

    /*
        Method that will provision an user when he/she registers from Audi Germany.
        Still in progress.
     */
    @RequestMapping("/provision")
    public String provisionUser(final HttpServletRequest request, final HttpServletResponse response) throws ProfileException {
        getLogger().trace("provisioning");
        final String username = request.getParameter(USERNAME_PARAM);
        final String password = request.getParameter(PASSWORD_PARAM);
        final String vin = request.getParameter(VIN_PARAM);
        final Email email = new Email.Builder().email(username).build();
        final Car car = new Car.Builder().withVin(vin).build();
        final Profile profile = new Profile.Builder().email(email).car(car).build();
        profileManagementService.register(profile, password);
        return "provision";
    }

    @RequestMapping("/session/invalidate")
    public String invalidateSession(final HttpServletRequest request, final HttpServletResponse response) {
        getLogger().trace("session invalidate");

        // Parameter name resume is coming from SP by convention, it a parameter set by ping federate
        // to be called after doing session invalidation operations.
        // we're just redirecting to it since we don't need to invalidate from this API.

        final String resumePath = request.getParameter("resume");
        final String redirectPath = "redirect:" + getServiceProviderHost() + resumePath;
        getLogger().info("Redirecting to... {} ", redirectPath);
        return redirectPath;
    }

    /*
       Method that will update an user when he/she updates his profile data from Audi Germany.
       Still in progress.
    */
    @RequestMapping("/update")
    public String updateUser(final HttpServletRequest request, final HttpServletResponse response) throws ProfileException {
        getLogger().trace("update");
        final String vin = request.getParameter(VIN_PARAM);

        if (vin != null) {
            final String username = request.getParameter(USERNAME_PARAM);
            final Email email = new Email.Builder().email(username).build();
            final Profile profile = profileManagementService.getProfile(email);

            final Car newCar = new Car.Builder().withVin(vin).build();
            final Profile newProfile = new Profile.Builder(profile).car(newCar).build();
            profileManagementService.updateProfile(profile, newProfile);
        }

        return "update";
    }

    private Logger getLogger() {
        return LOGGER;
    }

    public String getServiceProviderHost() {
        return serviceProviderHost;
    }

    public void setServiceProviderHost(final String serviceProviderHost) {
        this.serviceProviderHost = serviceProviderHost;
    }

    @RequestMapping("/{email}/profile")
    public ModelAndView getProfileFor(@PathVariable("email") final String email,
                                      final HttpServletResponse response) throws ProfileException {
        String decodedEmail = email;
        try {
            decodedEmail = URLDecoder.decode(email, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            getLogger().info("Unable to decode {}, trying to get the profile anyway.", email);
        }

        final Email emailToAsk = new Email.Builder().email(decodedEmail).build();

        final Profile profile = profileManagementService.getProfile(emailToAsk);
        final ModelAndView modelAndView = new ModelAndView("profile");
        if (profile == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            constructModel(decodedEmail, profile, modelAndView);
        }
        return modelAndView;
    }

    private void constructModel(final String decodedEmail,
                                final Profile profile,
                                final ModelAndView modelAndView) {
        final StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (final Car car : profile.getCars()) {
            if (isFirst) {
                isFirst = false;
            } else {
                builder.append(",");
            }
            builder.append(car.getVin());
        }

        modelAndView.addObject("vinnumber", builder.toString());
        modelAndView.addObject("firstName", profile.getName().getFirstName());
        modelAndView.addObject("lastName", profile.getName().getLastName());
        modelAndView.addObject("samlSubject", "US-" + getRandomNumber());
        modelAndView.addObject("email", decodedEmail);
    }

    protected int getRandomNumber() {
        return (int) (Math.random() * (10000));
    }

    @RequestMapping("insertMockCar")
    public String insertMockCar() throws ProfileException {
        final Email email = createDomainEmail("steve.lee@audi.com");
        final Profile profile = profileManagementService.getProfile(email);
        final Car car = createDomainCar();
        profileManagementService.addCar(profile, car);
        return "insertMockUser";
    }

    @RequestMapping("insertMockUser")
    public String insertMockUser() throws ProfileException {
        final Profile profile = createDomainProfile("gprslyons2@audi.com");
        /*final Token token =*/ profileManagementService.register(profile, "password", false);
//        profileManagementService.activateProfileFollowingRegistration(profile, token.getValue());
        return "insertMockUser";
    }


    public com.akqa.audi.user.domain.Profile createDomainProfile() {
        return new com.akqa.audi.user.domain.Profile.Builder().name(createDomainName()).address(createAddress())
                .location(createDomainLocation()).email(createDomainEmail()).secondaryEmail(createDomainEmail())
                .phone(createDomainPhone()).dealerId(createDomainDealerId())
                .preferences(createDomainPreferences()).build();
    }

    public com.akqa.audi.user.domain.Profile createDomainProfile(final String userEmail) {
        return new com.akqa.audi.user.domain.Profile.Builder().name(createDomainName()).address(createAddress())
                .location(createDomainLocation()).email(createDomainEmail(userEmail)).phone(createDomainPhone())
//                .secondaryEmail(createDomainEmail())
//                .dealerId(createDomainDealerId())
                .preferences(createDomainPreferences()).build();
    }

    public Car createDomainCar() {
        return new Car.Builder().withVin("WAUBA24B3XN104537").withBody("Sedan").withCarline("Allroad").withTrimline("")
                .withModelYear(1999).withEngine("6 Cylinder").withExterior("Exterior").withInterior("Interior")
                .withModelCode("ModelCode").withCsid("Csid").withConnectedEnabled(false).build();
    }


    public com.akqa.audi.user.domain.Profile createDomainProfileWithCar() {
        return new com.akqa.audi.user.domain.Profile.Builder().name(createDomainName()).address(createAddress())
                .location(createDomainLocation()).email(createDomainEmail(createDomainEmail().getEmail()))
                .secondaryEmail(createDomainEmail()).phone(createDomainPhone())
                .dealerId(createDomainDealerId()).car(createDomainCar()).build();
    }

    private Phone createDomainPhone() {
        return new Phone.Builder().phoneNumber(RandomStringUtils.randomNumeric(10)).type("WORK").build();
    }

    private DealerId createDomainDealerId() {
        return new DealerId.Builder().withId(RandomStringUtils.random(20)).build();
    }

    public com.akqa.audi.user.domain.Address createAddress() {
        return new com.akqa.audi.user.domain.Address.Builder().address1(RandomStringUtils.randomAlphanumeric(60))
                .city("San Francisco").state("CA").zip("94100").addressName("A5").build();
    }

    public Location createDomainLocation() {
        return new Location.Builder().latitude(37.7749f).longitude(122.4183f).build();
    }

    public Preferences createDomainPreferences() {
        final Preferences.Builder preferences = new Preferences.Builder();

        final List<Subject> subjects = new ArrayList<Subject>();
        subjects.add(new Subject.Builder().id(RandomStringUtils.random(10)).build());
        preferences.subjects(subjects);

        final List<Subscription> subscriptions = new ArrayList<Subscription>();
        subscriptions.add(new Subscription.Builder().id(RandomStringUtils.random(10)).build());
        preferences.subscriptions(subscriptions);

        final List<Model> models = new ArrayList<Model>();
        models.add(new Model.Builder().id(RandomStringUtils.random(10)).build());
        preferences.models(models);

        return preferences.build();
    }

    public Email createDomainEmail() {
        return new Email.Builder().email(RandomStringUtils.randomAlphanumeric(8) + "@test.server.com").build();
    }

    public Email createDomainEmail(final String userEmail) {
        return new Email.Builder().email(userEmail).build();
    }

    public Name createDomainName() {
        return new Name.Builder().firstName("First Name").lastName("Last Name").build();
    }
}