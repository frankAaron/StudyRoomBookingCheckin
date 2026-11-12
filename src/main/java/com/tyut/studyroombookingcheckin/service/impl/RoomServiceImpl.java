package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.entity.Building;
import com.tyut.studyroombookingcheckin.entity.Room;
import com.tyut.studyroombookingcheckin.entity.Seat;
import com.tyut.studyroombookingcheckin.mapper.BuildingMapper;
import com.tyut.studyroombookingcheckin.mapper.RoomMapper;
import com.tyut.studyroombookingcheckin.mapper.SeatMapper;
import com.tyut.studyroombookingcheckin.service.RoomService;
import com.tyut.studyroombookingcheckin.vo.RoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SeatMapper seatMapper;

    @Override
    public List<RoomVO> listByBuilding(Long buildingId) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getBuildingId, buildingId)
                .orderByAsc(Room::getFloor, Room::getName));
        List<RoomVO> vos = new ArrayList<>();
        for (Room r : rooms) {
            vos.add(toVO(r));
        }
        return vos;
    }

    @Override
    public RoomVO getById(Long id) {
        Room r = roomMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_EXIST);
        }
        return toVO(r);
    }

    @Override
    public RoomVO create(RoomVO vo) {
        Building building = buildingMapper.selectById(vo.getBuildingId());
        if (building == null) {
            throw new BusinessException(ErrorCode.BUILDING_NOT_EXIST);
        }
        Room r = new Room();
        r.setBuildingId(vo.getBuildingId());
        r.setName(vo.getName());
        r.setCode(vo.getCode());
        r.setFloor(vo.getFloor());
        r.setEquipment(vo.getEquipment());
        r.setOpenTime(vo.getOpenTime());
        r.setCloseTime(vo.getCloseTime());
        r.setTotalSeats(0);
        r.setAvailableSeats(0);
        r.setStatus(1);
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        roomMapper.insert(r);
        return toVO(r);
    }

    @Override
    public RoomVO update(Long id, RoomVO vo) {
        Room r = roomMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_EXIST);
        }
        r.setName(vo.getName());
        r.setFloor(vo.getFloor());
        r.setEquipment(vo.getEquipment());
        r.setOpenTime(vo.getOpenTime());
        r.setCloseTime(vo.getCloseTime());
        r.setUpdateTime(LocalDateTime.now());
        roomMapper.updateById(r);
        return toVO(r);
    }

    @Override
    public void delete(Long id) {
        Room r = roomMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_EXIST);
        }
        long seatCount = seatMapper.selectCount(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, id));
        if (seatCount > 0) {
            throw new BusinessException("请先删除该自习室下的所有座位");
        }
        roomMapper.deleteById(id);
    }

    @Override
    public Page<RoomVO> page(long current, long size, Long buildingId) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        if (buildingId != null) {
            wrapper.eq(Room::getBuildingId, buildingId);
        }
        wrapper.orderByAsc(Room::getFloor, Room::getName);
        Page<Room> page = roomMapper.selectPage(new Page<>(current, size), wrapper);
        Page<RoomVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<RoomVO> records = new ArrayList<>();
        for (Room r : page.getRecords()) {
            records.add(toVO(r));
        }
        voPage.setRecords(records);
        return voPage;
    }

    private RoomVO toVO(Room r) {
        RoomVO vo = new RoomVO();
        vo.setId(r.getId());
        vo.setBuildingId(r.getBuildingId());
        Building b = buildingMapper.selectById(r.getBuildingId());
        if (b != null) {
            vo.setBuildingName(b.getName());
        }
        vo.setName(r.getName());
        vo.setCode(r.getCode());
        vo.setFloor(r.getFloor());
        vo.setEquipment(r.getEquipment());
        vo.setOpenTime(r.getOpenTime());
        vo.setCloseTime(r.getCloseTime());
        Integer available = roomMapper.countAvailableSeats(r.getId());
        Integer total = roomMapper.countTotalActiveSeats(r.getId());
        vo.setAvailableSeats(available);
        vo.setTotalSeats(total);
        vo.setUtilizationRate(total > 0 ? Math.round((total - available) * 100.0 / total) / 100.0 : 0.0);
        return vo;
    }
}