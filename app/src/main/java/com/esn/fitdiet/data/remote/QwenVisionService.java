package com.esn.fitdiet.data.remote;

import com.esn.fitdiet.data.remote.dto.FoodItemDto;
import java.util.List;

/**
 * 拍照识别服务接口，应用内统一返回 Result<T>。
 */
public interface QwenVisionService {
    Result<List<FoodItemDto>> recognize(byte[] imageJpeg, String prompt);
}
