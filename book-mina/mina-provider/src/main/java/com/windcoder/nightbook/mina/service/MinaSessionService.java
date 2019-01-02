package com.windcoder.nightbook.mina.service;

import com.windcoder.nightbook.common.service.BaseService;
import com.windcoder.nightbook.mina.entity.MinaSession;
import com.windcoder.nightbook.mina.repository.MinaSessionRepository;
import org.springframework.stereotype.Service;

@Service
public class MinaSessionService extends BaseService<MinaSession,Long,MinaSessionRepository> {

}
