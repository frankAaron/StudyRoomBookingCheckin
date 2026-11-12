package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.entity.Building;
import com.tyut.studyroombookingcheckin.entity.Room;
import com.tyut.studyroombookingcheckin.mapper.BuildingMapper;
import com.tyut.studyroombookingcheckin.mapper.RoomMapper;
import com.tyut.studyroombookingcheckin.service.BuildingService;
import com.tyut.studyroombookingcheckin.vo.BuildingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingServiceImpl implements BuildingService {

    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;

    @Override
    public List<BuildingVO> listAll() {
        List<Building> buildings = buildingMapper.selectList(null);
        List<BuildingVO> vos = new ArrayList<>();
        for (Building b : buildings) {
            vos.add(toVO(b));
        }
        return vos;
    }

    @Override
    public BuildingVO getById(Long id) {
        Building b = buildingMapper.selectById(id);
        if (b == null) {
            throw new BusinessException(ErrorCode.BUILDING_NOT_EXIST);
        }
        return toVO(b);
    }

    @Override
    public BuildingVO create(BuildingVO vo) {
        Building b = new Building();
        b.setName(vo.getName());
        b.setCode(vo.getCode());
        b.setLocation(vo.getLocation());
        b.setDescription(vo.getDescription());
        b.setStatus(1);
        b.setCreateTime(LocalDateTime.now());
        b.setUpdateTime(LocalDateTime.now());
        buildingMapper.insert(b);
        return toVO(b);
    }

    @Override
    public BuildingVO update(Long id, BuildingVO vo) {
        Building b = buildingMapper.selectById(id);
        if (b == null) {
            throw new BusinessException(ErrorCode.BUILDING_NOT_EXIST);
        }
        b.setName(vo.getName());
        b.setLocation(vo.getLocation());
        b.setDescription(vo.getDescription());
        b.setUpdateTime(LocalDateTime.now());
        buildingMapper.updateById(b);
        return toVO(b);
    }

    @Override
    public void delete(Long id) {
        Building b = buildingMapper.selectById(id);
        if (b == null) {
            throw new BusinessException(ErrorCode.BUILDING_NOT_EXIST);
        }
        buildingMapper.deleteById(id);
    }

    private BuildingVO toVO(Building b) {
        BuildingVO vo = new BuildingVO();
        vo.setId(b.getId());
        vo.setName(b.getName());
        vo.setCode(b.getCode());
        vo.setLocation(b.getLocation());
        vo.setDescription(b.getDescription());
        long roomCount = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getBuildingId, b.getId()));
        vo.setRoomCount((int) roomCount);
        vo.setAvailableSeats(roomMapper.countAvailableSeats(b.getId()));
        return vo;
    }
}