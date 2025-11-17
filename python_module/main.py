from fastapi import FastAPI
from pydantic import BaseModel
import mysql.connector
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.vectorstores import FAISS

app = FastAPI()

# --- CẤU HÌNH DATABASE ---
db_config = {
    'user': 'root',
    'password': 'Qthanh0704@@', 
    'host': 'localhost',
    'database': 'smart_booking_db',
    'charset': 'utf8mb4'
}

# --- TẢI BỘ NHỚ AI ---
print("🔄 Đang khởi động Server AI...")
try:
    embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")
    # allow_dangerous_deserialization=True là bắt buộc để load file vector local
    vector_db = FAISS.load_local("vector_store", embeddings, allow_dangerous_deserialization=True)
    print("✅ Đã tải xong bộ nhớ! Sẵn sàng phục vụ.")
except Exception as e:
    print(f"⚠️ CẢNH BÁO: Không tìm thấy 'vector_store'. Hãy chạy 'python indexer.py' trước!")
    vector_db = None

class ChatRequest(BaseModel):
    user_id: str
    message: str

# --- HÀM LẤY LỊCH SỬ TÌM KIẾM ---
def get_user_history(user_id):
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        query = "SELECT searchTerm FROM search_history WHERE userId = %s ORDER BY createdAt DESC LIMIT 5"
        cursor.execute(query, (user_id,))
        results = cursor.fetchall()
        conn.close()
        if not results: return ""
        return ", ".join([row[0] for row in results if row[0]])
    except Exception as e:
        print(f"Lỗi DB History: {e}")
        return ""

# --- HÀM PHÁT HIỆN ĐỊA ĐIỂM ---
def detect_city_filter(user_message):
    # Map từ khóa người dùng nhập -> Tên chuẩn trong Database
    # (Thêm các biến thể viết tắt/không dấu vào đây)
    known_cities = {
        "đà lạt": "Đà Lạt",
        "da lat": "Đà Lạt",
        "hà nội": "Hà Nội",
        "ha noi": "Hà Nội",
        "đà nẵng": "Đà Nẵng",
        "da nang": "Đà Nẵng",
        "hồ chí minh": "Hồ Chí Minh",
        "sài gòn": "Hồ Chí Minh",
        "tphcm": "Hồ Chí Minh",
        "vũng tàu": "Vũng Tàu",
        "nha trang": "Nha Trang",
        "phú quốc": "Phú Quốc"
    }
    
    msg_lower = user_message.lower()
    for key, value in known_cities.items():
        if key in msg_lower:
            return value
    return None

# --- API CHÍNH ---
@app.post("/api/recommend")
async def recommend(req: ChatRequest):
    if not vector_db:
        return {"success": False, "message": "Server chưa có dữ liệu AI. Hãy chạy indexer.py trước."}

    # 1. Lấy lịch sử
    history = get_user_history(req.user_id)
    
    # 2. Tạo câu truy vấn kết hợp (Hybrid Query)
    if history:
        query = f"User likes: {history}. User asks: {req.message}"
    else:
        query = req.message

    # 3. Xử lý lọc địa điểm (Smart Filtering)
    target_city = detect_city_filter(req.message)
    docs = []
    filter_status = "No Filter"

    # [ƯU TIÊN 1] Tìm kiếm CÓ LỌC chính xác theo thành phố
    if target_city:
        print(f"🎯 Phát hiện địa điểm: '{target_city}'. Đang thử lọc dữ liệu...")
        # Chỉ lấy kết quả có metadata city trùng khớp
        docs = vector_db.similarity_search(query, k=5, filter={"city": target_city})
        
        if docs:
            filter_status = f"Filtered by {target_city}"
            print(f"✅ Tìm thấy {len(docs)} kết quả tại {target_city}.")
        else:
            print(f"⚠️ Không tìm thấy khách sạn nào có nhãn '{target_city}'. Đang chuyển sang tìm kiếm mở rộng...")
    
    # [ƯU TIÊN 2 - FALLBACK] Nếu lọc thất bại hoặc không có địa điểm -> Tìm trên toàn bộ
    # (Đảm bảo luôn trả về kết quả gần đúng nhất thay vì rỗng)
    if not docs:
        if target_city: filter_status = f"Fallback (Filter '{target_city}' failed)"
        print("🔎 Đang tìm kiếm trên toàn bộ hệ thống (Semantic Search)...")
        docs = vector_db.similarity_search(query, k=5)

    # 4. Chuẩn bị kết quả trả về
    results = []
    for doc in docs:
        results.append({
            "property_id": doc.metadata.get("property_id"),
            "city": doc.metadata.get("city"), # Trả về để debug
            "info": doc.page_content
        })
    
    return {
        "success": True,
        "user_history": history,
        "filter_status": filter_status,
        "recommendations": results
    }