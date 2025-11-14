package site.guiro.api.guide.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.guiro.api.guide.service.GuideService;

@RestController
@RequestMapping("/api/v1/guide")
@RequiredArgsConstructor
public class GuideController {

    private final GuideService guideService;

}
