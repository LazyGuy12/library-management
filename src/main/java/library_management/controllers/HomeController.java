package library_management.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import library_management.models.Book;
import library_management.repository.BookRepository;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {
    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/")
    public String index() {
        return "home";
    }

    @GetMapping("/api/books")
    @ResponseBody
    public Page<Book> getBooks(@RequestParam(defaultValue = "0") int page) {
        // Mỗi lần lấy 8 cuốn sách
        Pageable pageable = PageRequest.of(page, 8);
        return bookRepository.findAll(pageable);
    }
}
