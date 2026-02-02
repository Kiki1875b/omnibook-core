package com.sprint.omnibook.broker.domain.repository

import com.sprint.omnibook.broker.domain.Room
import org.springframework.data.jpa.repository.JpaRepository

interface RoomRepository : JpaRepository<Room, Long>
