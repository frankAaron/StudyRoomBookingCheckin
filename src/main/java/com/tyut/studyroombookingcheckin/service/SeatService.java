package com.tyut.studyroombookingcheckin.service;

import com.tyut.studyroombookingcheckin.vo.SeatVO;

import java.util.List;

public interface SeatService {

    List<SeatVO> listByRoom(Long roomId);

    SeatVO getById(Long id);

    SeatVO create(SeatVO vo);

    SeatVO update(Long id, SeatVO vo);

    void delete(Long id);

    void batchCreate(Long roomId, int rows, int cols);

    SeatVO changeStatus(Long id, Integer status);

    SeatVO getByQrcode(String qrcode);
}