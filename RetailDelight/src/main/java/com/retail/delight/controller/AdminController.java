package com.retail.delight.controller;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.retail.delight.dao.OrderDAO;
import com.retail.delight.dao.ProductDAO;
import com.retail.delight.entity.Product;
import com.retail.delight.form.ProductForm;
import com.retail.delight.model.OrderDetailInfo;
import com.retail.delight.model.OrderInfo;
import com.retail.delight.pagination.PaginationResult;
import com.retail.delight.validator.ProductFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Transactional
public class AdminController {

	@Autowired
	private OrderDAO orderDAO;

	@Autowired
	private ProductDAO productDAO;

	@Autowired
	private ProductFormValidator productFormValidator;

	private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

	//to trim spaces and pre-processing of any object
	@InitBinder
	public void myInitBinder(WebDataBinder dataBinder) {
		Object target = dataBinder.getTarget();
		if (target == null) {
			return;
		}
		System.out.println("Target=" + target);

		if (target.getClass() == ProductForm.class) {
			dataBinder.setValidator(productFormValidator);
		}
	}

	// GET: Show Login Page
	@RequestMapping(value = { "/admin/login" }, method = RequestMethod.GET)
	public String login(Model model) {
		logger.info("The admin is allowed access to the home page");
		return "login";
	}

	@RequestMapping(value = { "/admin/accountInfo" }, method = RequestMethod.GET)
	public String accountInfo(Model model) {
		logger.info("Fetching the user details for the admin");
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		/*      System.out.println(userDetails.getPassword());
      System.out.println(userDetails.getUsername());
      System.out.println(userDetails.isEnabled());*/
		logger.debug("The user details for the admin has been fetched, {}",userDetails);
		model.addAttribute("userDetails", userDetails);
		return "accountInfo";
	}

	@RequestMapping(value = { "/admin/orderList" }, method = RequestMethod.GET)
	public String orderList(Model model, //
			@RequestParam(value = "page", defaultValue = "1") String pageStr) {
		int page = 1;
		try {
			page = Integer.parseInt(pageStr);
		} catch (Exception e) {
		}
		final int MAX_RESULT = 20;
		final int MAX_NAVIGATION_PAGE = 10;
		logger.info("Fetching the order list from the database");
		PaginationResult<OrderInfo> paginationResult //
		= orderDAO.listOrderInfo(page, MAX_RESULT, MAX_NAVIGATION_PAGE);
		logger.debug("The order list has been succesfully fetched with {} orders",paginationResult.getTotalRecords());
		model.addAttribute("paginationResult", paginationResult);
		return "orderList";
	}

	// GET: Show product.
	@RequestMapping(value = { "/admin/product" }, method = RequestMethod.GET)
	public String product(Model model, @RequestParam(value = "code", defaultValue = "") String code) {

		ProductForm productForm = null;
		logger.info("Validating the length of the product code");
		if (code != null && code.length() > 0) {
			Product product = productDAO.findProduct(code);			
			logger.info("Validating the product availablity");
			if (product != null) {
				logger.debug("The product to be fetched is {} and is available", product.getName());
				productForm = new ProductForm(product);
			}
		}
		if (productForm == null) {
			productForm = new ProductForm();
			productForm.setNewProduct(true);
			logger.error("The product could not be added to the database");
		}
		model.addAttribute("productForm", productForm);
		logger.info("The product has been succesfully fetched and loaded from the product list");
		return "product";
	}

	// POST: Save product
	@RequestMapping(value = { "/admin/product" }, method = RequestMethod.POST)
	public String productSave(Model model, //
			@ModelAttribute("productForm") @Validated ProductForm productForm, //
			BindingResult result, //
			final RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			logger.error("The product {} could not be added to the database",productForm.getName());
			return "product";
		}
		try {
			productDAO.save(productForm);
			logger.debug("Adding of product {} to the database has been succesfully done",productForm.getName());
		} catch (Exception e) {
			Throwable rootCause = ExceptionUtils.getRootCause(e);
			String message = rootCause.getMessage();
			model.addAttribute("errorMessage", message);
			logger.error("The product {} could not be added to the database",productForm.getName());
			// Show product form.
			return "product";
		}

		return "redirect:/productListManager";
	}
	//Get: to get order details
	@RequestMapping(value = { "/admin/order" }, method = RequestMethod.GET)
	public String orderView(Model model, @RequestParam("orderId") String orderId) {
		OrderInfo orderInfo = null;

		if (orderId != null) {
			logger.debug("The order details for the order id {} is available",orderId);
			orderInfo = this.orderDAO.getOrderInfo(orderId);
		}
		if (orderInfo == null) {
			logger.error("Fetching the order details from the database for the order id {} not possible",orderId);
			return "redirect:/admin/orderList";
		}
		List<OrderDetailInfo> details = this.orderDAO.listOrderDetailInfos(orderId);
		orderInfo.setDetails(details);

		model.addAttribute("orderInfo", orderInfo);
		logger.debug("The order details has been fetched from the database for the order id {}",orderId);
		logger.info("The order details has been succesfully displayed to the admin for the order id {}",orderId);
		return "order";
	}

}
