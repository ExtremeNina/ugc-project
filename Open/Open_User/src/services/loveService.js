import axios from 'axios';

/**
 * 点赞服务类，封装点赞相关的API调用
 */
class LoveService {
  /**
   * 发送点赞/取消点赞请求（使用同一接口）
   * @param {Object} loveData 点赞数据
   * @param {number} loveData.id 实体ID（文章ID或评论ID）
   * @param {number} loveData.loveTypeId 点赞类型ID
   * @returns {Promise} 响应Promise对象
   */
  static async toggleLove(loveData) {
    try {
      // 获取token
      const token = localStorage.getItem('token') || sessionStorage.getItem('token');
      
      // 准备请求配置
      const config = {};
      if (token) {
        // 移除可能存在的'Bearer '前缀（如果后端已包含）
        const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
        config.headers = {
          'Authorization': `Bearer ${cleanToken}`
        };
      }
      
      // 发送点赞/取消点赞请求到新接口
      const response = await axios.post(
        'http://localhost:8081/user/api/Interaction/likes',
        loveData,
        config
      );
      
      console.log('点赞/取消点赞请求成功:', response.data);
      return response.data;
    } catch (error) {
      console.error('点赞/取消点赞请求失败:', error);
      
      // 处理不同类型的错误
      if (error.response) {
        // 服务器返回了错误状态码
        if (error.response.status === 401) {
          // 未授权错误，可能需要重新登录
          alert('请先登录后再进行点赞操作');
        }
        throw new Error(error.response.data?.message || '操作失败');
      } else if (error.request) {
        // 请求已发送但没有收到响应
        throw new Error('网络错误，请检查您的网络连接');
      } else {
        // 设置请求时发生错误
        throw new Error('操作失败');
      }
    }
  }
  
  /**
   * 保留原有like方法以兼容旧代码
   */
  static async like(loveData) {
    return this.toggleLove(loveData);
  }
}

export default LoveService;