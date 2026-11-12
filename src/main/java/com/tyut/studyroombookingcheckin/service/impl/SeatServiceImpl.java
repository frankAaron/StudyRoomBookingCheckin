package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.Constants;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.entity.Room;
import com.tyut.studyroombookingcheckin.entity.Seat;
import com.tyut.studyroombookingcheckin.mapper.RoomMapper;
import com.tyut.studyroombookingcheckin.mapper.SeatMapper;
import com.tyut.studyroombookingcheckin.service.SeatService;
import com.tyut.studyroombookingcheckin.vo.SeatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;

    @Override
    public List<SeatVO> listByRoom(Long roomId) {
        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getRoomId, roomId));
        // row 为 MySQL 保留字，排序在内存中完成，避免 ORDER BY row 语法错误
        seats.sort((a, b) -> {
            int ra = a.getSeatRow() == null ? 0 : a.getSeatRow();
            int rb = b.getSeatRow() == null ? 0 : b.getSeatRow();
            if (ra != rb) {
                return ra - rb;
            }
            int ca = a.getCol() == null ? 0 : a.getCol();
            int cb = b.getCol() == null ? 0 : b.getCol();
            return ca - cb;
        });
        List<SeatVO> vos = new ArrayList<>();
        for (Seat s : seats) {
            vos.add(toVO(s));
        }
        return vos;
    }

    @Override
    public SeatVO getById(Long id) {
        Seat s = seatMapper.selectById(id);
        if (s == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_EXIST);
        }
        return toVO(s);
    }

    @Override
    public SeatVO create(SeatVO vo) {
        Room room = roomMapper.selectById(vo.getRoomId());
        if (room == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_EXIST);
        }
        Seat s = new Seat();
        s.setRoomId(vo.getRoomId());
        s.setSeatNo(vo.getSeatNo());
        s.setSeatRow(vo.getRow());
        s.setCol(vo.getCol());
        s.setStatus(Constants.SEAT_STATUS_FREE);
        s.setQrcode(UUID.randomUUID().toString());
        s.setCreateTime(LocalDateTime.now());
        s.setUpdateTime(LocalDateTime.now());
        seatMapper.insert(s);
        return toVO(s);
    }

    @Override
    public SeatVO update(Long id, SeatVO vo) {
        Seat s = seatMapper.selectById(id);
        if (s == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_EXIST);
        }
        s.setSeatNo(vo.getSeatNo());
        s.setSeatRow(vo.getRow());
        s.setCol(vo.getCol());
        s.setUpdateTime(LocalDateTime.now());
        seatMapper.updateById(s);
        return toVO(s);
    }

    @Override
    public void delete(Long id) {
        seatMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchCreate(Long roomId, int rows, int cols) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_EXIST);
        }
        int total = 0;
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Seat s = new Seat();
                s.setRoomId(roomId);
                s.setSeatNo(r + "-" + c);
                s.setSeatRow(r);
                s.setCol(c);
                s.setStatus(Constants.SEAT_STATUS_FREE);
                s.setQrcode(UUID.randomUUID().toString());
                s.setCreateTime(LocalDateTime.now());
                s.setUpdateTime(LocalDateTime.now());
                seatMapper.insert(s);
                total++;
            }
        }
        room.setTotalSeats(room.getTotalSeats() + total);
        room.setAvailableSeats(room.getAvailableSeats() + total);
        room.setUpdateTime(LocalDateTime.now());
        roomMapper.updateById(room);
    }

    @Override
    public SeatVO changeStatus(Long id, Integer status) {
        Seat s = seatMapper.selectById(id);
        if (s == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_EXIST);
        }
        s.setStatus(status);
        s.setUpdateTime(LocalDateTime.now());
        seatMapper.updateById(s);
        return toVO(s);
    }

    @Override
    public SeatVO getByQrcode(String qrcode) {
        Seat s = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getQrcode, qrcode));
        if (s == null) {
            throw new BusinessException(ErrorCode.SEAT_NOT_EXIST);
        }
        return toVO(s);
    }

    private SeatVO toVO(Seat s) {
        SeatVO vo = new SeatVO();
        vo.setId(s.getId());
        vo.setRoomId(s.getRoomId());
        vo.setSeatNo(s.getSeatNo());
        vo.setRow(s.getSeatRow());
        vo.setCol(s.getCol());
        vo.setStatus(s.getStatus());
        vo.setOccupiedUserId(s.getOccupiedUserId());
        vo.setQrcode(s.getQrcode());
        return vo;
    }
}