"""Generates src/main/resources/db/changelog/changes/0100-demo-data.sql (Liquibase context: demo).

Run: python3 scripts/generate_demo_seed.py   (only before the changesets have run anywhere you care about;
Liquibase checksums them, so after that add a NEW changeset instead of regenerating)."""
import json
from datetime import date, timedelta
from decimal import Decimal

import os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/db/changelog/changes/0100-demo-data.sql")
IMG = "https://images.unsplash.com/{}?w=1200&q=80&auto=format&fit=crop"

def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "b'1'" if v else "b'0'"
    if isinstance(v, (int, Decimal)):
        return str(v)
    if isinstance(v, date):
        return f"'{v.isoformat()}'"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"

def insert(table, cols, rows):
    lines = [f"INSERT INTO {table} ({', '.join(cols)}) VALUES"]
    lines.append(",\n".join("  (" + ", ".join(r) + ")" for r in rows) + ";")
    return "\n".join(lines)

def raw(s):  # value written verbatim (SQL expression)
    return s

# ---------------------------------------------------------------- users
HASH = {
    "Admin@123": "$2a$10$xMhsfFZCPinKFZFxX4t/nOWHlpgb3anOXlIhZO6MCnMW.sIN52sIe",
    "Owner@123": "$2a$10$WLck6nH0qtDJVqdcJAkJZe05LuyhtyeFAUZ535.ZNN0/o9opf/EnK",
    "Customer@123": "$2a$10$KSarVZMGXadybIjHKp58peVSOiJqtGDnNneW85dyRAD.UFLmPEzSC",
}
U = lambda n: f"00000000-0000-4000-8000-{n:012d}"
users = [  # id, email, password, name, phone, roles, gender, dob, city
    (U(1), "admin@travelmate.vn", "Admin@123", "Quản trị viên TravelMate", "0901000001", ["ADMIN", "CUSTOMER"], "Nam", date(1990, 3, 12), "Hà Nội"),
    (U(2), "owner@travelmate.vn", "Owner@123", "Nguyễn Văn Minh", "0901000002", ["OWNER", "CUSTOMER"], "Nam", date(1985, 7, 20), "Hà Nội"),
    (U(3), "owner2@travelmate.vn", "Owner@123", "Trần Thị Lan", "0901000003", ["OWNER", "CUSTOMER"], "Nữ", date(1988, 11, 2), "Đà Nẵng"),
    (U(4), "customer@travelmate.vn", "Customer@123", "Lê Hoàng Nam", "0901000004", ["CUSTOMER"], "Nam", date(1996, 5, 9), "Hà Nội"),
    (U(5), "customer2@travelmate.vn", "Customer@123", "Phạm Thu Hà", "0901000005", ["CUSTOMER"], "Nữ", date(1994, 1, 25), "TP. Hồ Chí Minh"),
    (U(6), "customer3@travelmate.vn", "Customer@123", "Hoàng Đức Anh", "0901000006", ["CUSTOMER"], "Nam", date(1999, 9, 14), "Hải Phòng"),
]

# ---------------------------------------------------------------- properties
# id, owner, name, type, status, address, district, city(=filter value), province, lat, lng, description, cover+gallery photo ids, policy
properties = [
    (1, U(2), "Hanoi Old Quarter Boutique Hotel", "HOTEL", "APPROVE", "25 Hàng Bạc, Hoàn Kiếm, Hà Nội", "Quận Hoàn Kiếm", "Thành phố Hà Nội", "Hà Nội", "21.034000", "105.852000",
     "Khách sạn boutique nằm giữa lòng phố cổ Hà Nội, chỉ 5 phút đi bộ tới Hồ Hoàn Kiếm. Phòng nghỉ ấm cúng mang phong cách Đông Dương, phục vụ bữa sáng phở truyền thống.",
     ["photo-1542314831-068cd1dbfeeb", "photo-1582719478250-c89cae4dc85b", "photo-1590490360182-c33d57733427"]),
    (2, U(2), "Sapa Valley View Homestay", "HOMESTAY", "APPROVE", "Bản Cát Cát, Sa Pa, Lào Cai", "Thị xã Sa Pa", "Tỉnh Lào Cai", "Lào Cai", "22.325600", "103.835800",
     "Homestay gỗ nguyên căn nhìn ra thung lũng Mường Hoa, lý tưởng cho nhóm bạn và gia đình. Có lò sưởi, bếp riêng và sân hiên ngắm ruộng bậc thang.",
     ["photo-1564013799919-ab600027ffc6", "photo-1522708323590-d24dbb6b0267", "photo-1505693416388-ac5ce068fe85"]),
    (3, U(2), "Ha Long Bay Pearl Hotel", "HOTEL", "APPROVE", "12 Đường Hạ Long, Bãi Cháy, Hạ Long, Quảng Ninh", "Phường Bãi Cháy", "Tỉnh Quảng Ninh", "Quảng Ninh", "20.952000", "107.045000",
     "Khách sạn 4 sao hướng vịnh Hạ Long với hồ bơi vô cực trên tầng thượng. Gần cảng tàu du lịch và khu vui chơi Sun World.",
     ["photo-1506929562872-bb421503ef21", "photo-1611892440504-42a792e24d32", "photo-1551882547-ff40c63fe5fa"]),
    (4, U(2), "Da Lat Pine Hill Villa", "VILLA", "APPROVE", "18 Trần Hưng Đạo, Phường 10, Đà Lạt, Lâm Đồng", "Thành phố Đà Lạt", "Tỉnh Lâm Đồng", "Lâm Đồng", "11.940000", "108.458000",
     "Biệt thự nguyên căn giữa đồi thông Đà Lạt, 4 phòng ngủ, sân vườn BBQ và phòng khách có lò sưởi. Cách chợ Đà Lạt 10 phút lái xe.",
     ["photo-1613490493576-7fde63acd811", "photo-1600585154340-be6161a56a0c", "photo-1596394516093-501ba68a0ba6"]),
    (5, U(3), "Saigon Riverside Hotel", "HOTEL", "APPROVE", "88 Tôn Đức Thắng, Bến Nghé, Quận 1, TP. Hồ Chí Minh", "Quận 1", "Thành phố Hồ Chí Minh", "Hồ Chí Minh", "10.776900", "106.706000",
     "Khách sạn ven sông Sài Gòn ngay trung tâm Quận 1, đi bộ tới phố đi bộ Nguyễn Huệ. Có quầy bar tầng thượng ngắm hoàng hôn.",
     ["photo-1583417319070-4a69db38a482", "photo-1631049307264-da0ec9d70304", "photo-1618773928121-c32242e63f39"]),
    (6, U(3), "Da Nang Beachfront Resort", "RESORT", "APPROVE", "200 Võ Nguyên Giáp, Mỹ An, Ngũ Hành Sơn, Đà Nẵng", "Quận Ngũ Hành Sơn", "Thành phố Đà Nẵng", "Đà Nẵng", "16.047000", "108.247000",
     "Resort sát biển Mỹ Khê với bãi biển riêng, spa và ba hồ bơi. Thuận tiện đi Hội An, Bà Nà Hills và Cầu Vàng.",
     ["photo-1520250497591-112f2f40a3f4", "photo-1559592413-7cec4d0cae2b", "photo-1571896349842-33c89424de2d"]),
    (7, U(3), "Hoi An Lantern Homestay", "HOMESTAY", "APPROVE", "45 Nguyễn Thị Minh Khai, Hội An, Quảng Nam", "Thành phố Hội An", "Tỉnh Quảng Nam", "Quảng Nam", "15.879000", "108.326000",
     "Nhà cổ được cải tạo thành homestay nguyên căn, cách phố cổ Hội An 3 phút đi bộ. Có xe đạp miễn phí và lớp học nấu ăn cuối tuần.",
     ["photo-1528127269322-539801943592", "photo-1560448204-e02f11c3d0e2", "photo-1586023492125-27b2c045efd7"]),
    (8, U(3), "Phu Quoc Sunset Resort", "RESORT", "APPROVE", "Bãi Trường, Dương Tơ, Phú Quốc, Kiên Giang", "Thành phố Phú Quốc", "Tỉnh Kiên Giang", "Kiên Giang", "10.172000", "103.967000",
     "Resort nghỉ dưỡng trên Bãi Trường với các bungalow hướng biển, nổi tiếng với hoàng hôn đẹp nhất đảo ngọc.",
     ["photo-1540206351-d6465b3ac5c1", "photo-1540541338287-41700207dee6", "photo-1566073771259-6a8506099945"]),
    (9, U(3), "Mui Ne Ocean Villa", "VILLA", "PENDING", "79 Nguyễn Đình Chiểu, Hàm Tiến, Phan Thiết, Bình Thuận", "Thành phố Phan Thiết", "Tỉnh Bình Thuận", "Bình Thuận", "10.947000", "108.204000",
     "Biệt thự biển mới khai trương tại Mũi Né, đang chờ quản trị viên duyệt.",
     ["photo-1584132967334-10e028bd69f7", "photo-1600607687939-ce8a6c25118c"]),
]
amenity_sets = {
    1: ["wifi", "ac", "reception_24h", "non_smoking", "airport_transfer"],
    2: ["wifi", "parking", "pets", "non_smoking"],
    3: ["pool", "wifi", "gym", "ac", "reception_24h", "parking"],
    4: ["wifi", "parking", "pets", "ac"],
    5: ["pool", "wifi", "gym", "spa", "ac", "reception_24h", "airport_transfer"],
    6: ["pool", "wifi", "gym", "spa", "sauna", "parking", "reception_24h", "airport_transfer"],
    7: ["wifi", "ac", "non_smoking"],
    8: ["pool", "wifi", "spa", "parking", "airport_transfer", "reception_24h"],
    9: ["pool", "wifi", "parking"],
}
# roomId, propertyId, name, category, capacity, area, weekday, weekend, amount, photo, room amenities
rooms = [
    (1, 1, "Phòng Standard Phố Cổ", "STANDARD", 2, 22, 650000, 850000, 6, "photo-1590490360182-c33d57733427", ["tv", "ac", "wifi", "non_smoking"]),
    (2, 1, "Phòng Deluxe Hướng Phố", "DELUXE", 2, 30, 950000, 1200000, 4, "photo-1582719478250-c89cae4dc85b", ["tv", "ac", "wifi", "minibar", "balcony"]),
    (3, 1, "Family Suite", "SUITE", 4, 45, 1600000, 2000000, 2, "photo-1595576508898-0ad5c879a061", ["tv", "ac", "wifi", "minibar", "bathtub", "tea_coffee"]),
    (4, 2, "Nguyên căn Valley View", "WHOLE", 6, 120, 1800000, 2200000, 1, "photo-1522708323590-d24dbb6b0267", ["wifi", "tea_coffee", "balcony"]),
    (5, 3, "Phòng Deluxe Hướng Vịnh", "DELUXE", 2, 32, 1300000, 1600000, 8, "photo-1611892440504-42a792e24d32", ["tv", "ac", "wifi", "minibar", "balcony"]),
    (6, 3, "Suite Panorama", "SUITE", 3, 55, 2400000, 2900000, 2, "photo-1578683010236-d716f9a3f461", ["tv", "ac", "wifi", "minibar", "bathtub", "balcony"]),
    (7, 4, "Nguyên căn Pine Hill", "WHOLE", 8, 250, 3500000, 4500000, 1, "photo-1600585154340-be6161a56a0c", ["tv", "wifi", "tea_coffee", "bathtub", "balcony"]),
    (8, 5, "Phòng Superior City View", "STANDARD", 2, 25, 1100000, 1300000, 10, "photo-1631049307264-da0ec9d70304", ["tv", "ac", "wifi", "tea_coffee"]),
    (9, 5, "Phòng Deluxe River View", "DELUXE", 2, 33, 1700000, 2000000, 6, "photo-1618773928121-c32242e63f39", ["tv", "ac", "wifi", "minibar", "bathtub"]),
    (10, 6, "Phòng Deluxe Hướng Biển", "DELUXE", 2, 38, 2200000, 2700000, 12, "photo-1571896349842-33c89424de2d", ["tv", "ac", "wifi", "minibar", "balcony"]),
    (11, 6, "Ocean Suite", "SUITE", 4, 70, 3800000, 4500000, 4, "photo-1559628233-100c798642d4", ["tv", "ac", "wifi", "minibar", "bathtub", "balcony", "tea_coffee"]),
    (12, 7, "Nguyên căn Lantern House", "WHOLE", 5, 90, 1500000, 1800000, 1, "photo-1560448204-e02f11c3d0e2", ["ac", "wifi", "tea_coffee"]),
    (13, 8, "Bungalow Hướng Biển", "DELUXE", 2, 40, 2500000, 3100000, 10, "photo-1540541338287-41700207dee6", ["tv", "ac", "wifi", "minibar", "balcony"]),
    (14, 8, "Pool Villa", "SUITE", 4, 95, 5200000, 6200000, 3, "photo-1566073771259-6a8506099945", ["tv", "ac", "wifi", "minibar", "bathtub", "balcony"]),
    (15, 9, "Nguyên căn Ocean Villa", "WHOLE", 10, 300, 4200000, 5000000, 1, "photo-1584132967334-10e028bd69f7", ["tv", "ac", "wifi", "balcony"]),
]
ROOM = {r[0]: r for r in rooms}

def price(room_id, cin, cout):
    r = ROOM[room_id]
    wd, we = Decimal(r[6]), Decimal(max(r[6], r[7]))
    total, d = Decimal(0), cin
    while d < cout:
        total += we if d.weekday() >= 5 else wd
        d += timedelta(days=1)
    return total

# bookingId, user, roomId, checkIn, checkOut, guests, status, paymentStatus, method
bookings = [
    (1, U(4), 2, date(2026, 8, 10), date(2026, 8, 12), 2, "COMPLETED", "APPROVED", "VNPAY"),
    (2, U(4), 10, date(2026, 11, 20), date(2026, 11, 23), 2, "CONFIRMED", "APPROVED", "MOMO"),
    (3, U(4), 9, date(2026, 10, 30), date(2026, 11, 1), 2, "CANCELLED", "REFUND_REQUESTED", "VNPAY"),
    (4, U(5), 13, date(2026, 7, 14), date(2026, 7, 17), 2, "COMPLETED", "APPROVED", "ZALOPAY"),
    (5, U(5), 7, date(2026, 9, 5), date(2026, 9, 7), 6, "COMPLETED", "APPROVED", "QR_CODE"),
    (6, U(5), 12, date(2026, 12, 24), date(2026, 12, 27), 4, "CONFIRMED", "APPROVED", "VNPAY"),
    (7, U(6), 4, date(2026, 9, 18), date(2026, 9, 20), 4, "COMPLETED", "APPROVED", "MOMO"),
    (8, U(6), 5, date(2026, 8, 22), date(2026, 8, 24), 2, "COMPLETED", "APPROVED", "VNPAY"),
]
# ratingId, bookingId, stars, comment, pinned
ratings = [
    (1, 1, 5, "Vị trí tuyệt vời ngay phố cổ, nhân viên thân thiện, bữa sáng phở rất ngon. Chắc chắn sẽ quay lại!", True),
    (2, 4, 4, "Bungalow sạch sẽ, hoàng hôn đẹp mê hồn. Đồ ăn ở nhà hàng hơi đắt nhưng xứng đáng.", False),
    (3, 5, 5, "Biệt thự rộng rãi, sân vườn đẹp, cả nhà rất thích buổi BBQ tối. Chủ nhà hỗ trợ nhiệt tình.", True),
    (4, 7, 4, "View ruộng bậc thang đẹp như tranh, homestay ấm cúng. Đường vào hơi dốc nên cần đi xe máy cẩn thận.", False),
    (5, 8, 5, "Phòng hướng vịnh cực đẹp, hồ bơi tầng thượng là điểm cộng lớn. Gần cảng tàu rất tiện.", False),
]

users_by_id = {u[0]: u for u in users}
prop_by_id = {p[0]: p for p in properties}

# denormalised property rating / reviewCount
prop_stats = {}
for rid, bid, stars, _, _ in ratings:
    pid = ROOM[[b for b in bookings if b[0] == bid][0][2]][1]
    prop_stats.setdefault(pid, []).append(stars)

# points & rank from completed bookings (1 point / 1,000 VND, BookingService.checkOutBooking)
points = {}
for b in bookings:
    if b[6] == "COMPLETED":
        points[b[1]] = points.get(b[1], 0) + int(price(b[2], b[3], b[4]) / 1000)
def rank(p):
    return "DIAMOND" if p >= 10000 else "GOLD" if p >= 5000 else "SILVER" if p >= 1000 else "BRONZE"

S = ["--liquibase formatted sql", "",
     "-- Demo dataset for local development and demos. Runs only with the Liquibase context `demo`",
     "-- (LIQUIBASE_CONTEXTS=demo). Generated by a script: prices follow BookingService (weekend nights use",
     "-- weekendPrice), points = completed spend / 1,000 VND, property rating = average of seeded reviews.",
     "-- Accounts: admin@ / owner@ / owner2@ / customer@ / customer2@ / customer3@travelmate.vn",
     "-- Passwords: Admin@123, Owner@123, Customer@123.", ""]

def changeset(cid, comment, body, rollback):
    S.append(f"--changeset travelmate:{cid} context:demo")
    S.append("--preconditions onFail:MARK_RAN")
    S.append("--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM users WHERE email IN (" + ", ".join(q(u[1]) for u in users) + ")"
             if cid.endswith("users") else
             "--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM users WHERE userId = '00000000-0000-4000-8000-000000000001'")
    S.append(f"--comment: {comment}")
    S.append(body)
    S.append(f"--rollback {rollback}")
    S.append("")

# users + roles + details
rows = []
for uid, email, pw, name, phone, roles, g, dob, city in users:
    pts = points.get(uid, 0)
    rows.append([q(uid), q(email), q(HASH[pw]), q(name), q(phone), "b'1'", "b'0'", q("ACTIVE"), q("local"), str(pts), q(rank(pts)), "NOW(6)", "NOW(6)"])
body = insert("users", ["userId", "email", "passwordHash", "fullName", "phoneNumber", "isEmailVerified", "twoFactorEnabled", "status", "provider", "points", "membershipRank", "createdAt", "updatedAt"], rows)
body += "\n" + "\n".join(
    f"INSERT INTO userRoles (userId, roleId) SELECT {q(u[0])}, roleId FROM roles WHERE roleName IN ({', '.join(q(r) for r in u[5])});" for u in users)
body += "\n" + insert("userdetail", ["userId", "gender", "dateOfBirth", "city", "country", "isActive", "createdAt", "updatedAt"],
                      [[q(u[0]), q(u[6]), q(u[7]), q(u[8]), q("Việt Nam"), "b'1'", "NOW(6)", "NOW(6)"] for u in users])
changeset("0100-demo-users", "Demo accounts (admin, 2 owners, 3 customers) with roles and profiles", body,
          "DELETE FROM userdetail WHERE userId LIKE '00000000-0000-4000-8000-%'; DELETE FROM userRoles WHERE userId LIKE '00000000-0000-4000-8000-%'; DELETE FROM users WHERE userId LIKE '00000000-0000-4000-8000-%';")

# owner applications: two approved, one pending
doc = IMG.format("photo-1557750255-c76072a7aad1")
body = insert("registerOwner", ["registerOwnerid", "userId", "permanent_address", "hometown_address", "cardFrontImage", "cardBackImage", "businessLicenseImage", "businessLicenseNumber", "personalIdCard", "personalDob", "status", "adminReason", "reviewed_by_admin_id", "createAt", "reviewed_at"], [
    [str(1), q(U(2)), q("25 Hàng Bạc, Hoàn Kiếm, Hà Nội"), q("Nam Định"), q(doc), q(doc), q(doc), q("0109876543"), q("001085012345"), q(date(1985, 7, 20)), q("APPROVED"), q("Hồ sơ hợp lệ"), q(U(1)), "'2026-05-02 09:00:00'", "'2026-05-03 10:30:00'"],
    [str(2), q(U(3)), q("200 Võ Nguyên Giáp, Đà Nẵng"), q("Quảng Nam"), q(doc), q(doc), q(doc), q("0401234567"), q("048188012345"), q(date(1988, 11, 2)), q("APPROVED"), q("Hồ sơ hợp lệ"), q(U(1)), "'2026-05-10 14:00:00'", "'2026-05-11 08:15:00'"],
    [str(3), q(U(6)), q("12 Lạch Tray, Ngô Quyền, Hải Phòng"), q("Hải Phòng"), q(doc), q(doc), q(doc), q("0201122334"), q("031099012345"), q(date(1999, 9, 14)), q("PENDING"), "NULL", "NULL", "'2026-10-08 16:45:00'", "NULL"],
])
changeset("0101-demo-owner-applications", "Owner applications: 2 approved, 1 waiting for admin review", body, "DELETE FROM registerOwner WHERE registerOwnerid IN (1, 2, 3);")

# properties, details, policies, images, amenities
rows = []
for p in properties:
    pid, owner, name, ptype, status, addr, district, city, province, lat, lng, desc, photos = p
    stars = prop_stats.get(pid, [])
    avg = Decimal(sum(stars)) / len(stars) if stars else Decimal(0)
    rows.append([str(pid), q(owner), q(name), q(ptype), q(status), "b'1'" if status == "APPROVE" else "b'0'", q(addr), q(district), q(city), q(province), q("Việt Nam"),
                 lat, lng, q(desc), q(f"0236{pid:06d}"), q(f"stay{pid}@travelmate.vn"), str(avg.quantize(Decimal("0.1"))), str(len(stars)), "CURDATE()", "CURDATE()"])
body = insert("properties", ["propertyId", "userId", "propertyName", "propertyType", "propertyStatus", "isActive", "address", "district", "city", "province", "country",
                             "latitude", "longitude", "description", "phoneContact", "emailContact", "rating", "reviewCount", "createdAt", "updatedAt"], rows)
body += "\n" + insert("property_details", ["propertyId", "area"], [[str(p[0]), str(sum(r[5] * r[8] for r in rooms if r[1] == p[0]) + 150)] for p in properties])
pol = []
for p in properties:
    pid, ptype = p[0], p[3]
    whole = ptype in ("VILLA", "HOMESTAY")
    pol.append([str(pid), "'14:00:00'", "'12:00:00'", "b'1'" if whole else "b'0'", q("Cho phép thú cưng nhỏ, phụ thu 200.000₫/đêm." if whole else "Không nhận thú cưng."),
                "b'0'", q("Không hút thuốc trong phòng."), "b'1'", q("Trẻ em dưới 6 tuổi ở miễn phí khi dùng giường có sẵn."),
                "b'1'" if pid % 3 else "b'0'", str(3 if pid % 3 else 0), q("Miễn phí hủy trước 3 ngày; sau đó tính phí 30%." if pid % 3 else "Không hoàn tiền khi hủy."),
                "b'1'", q("Thanh toán toàn bộ khi đặt phòng."), "b'1'" if whole else "b'0'", "1000000" if whole else "NULL",
                q("Đặt cọc 1.000.000₫, hoàn lại khi trả phòng." if whole else None), "18", q("22:00 - 06:00")])
body += "\n" + insert("property_policies", ["propertyId", "checkInTime", "checkOutTime", "petsAllowed", "petPolicyDescription", "smokingAllowed", "smokingPolicyDescription",
                                             "childrenAllowed", "childrenPolicyDescription", "allowFreeCancellation", "freeCancellationDays", "cancellationPolicyDescription",
                                             "requiresPrepayment", "prepaymentPolicy", "securityDepositRequired", "securityDepositAmount", "securityDepositDescription", "minimumAge", "quietHours"], pol)
img_rows, n = [], 0
for p in properties:
    for i, ph in enumerate(p[12]):
        n += 1
        img_rows.append([str(n), str(p[0]), q(IMG.format(ph)), "b'1'" if i == 0 else "b'0'", "b'1'"])
body += "\n" + insert("propertyimage", ["propertyImageId", "propertyId", "imageUrl", "isCover", "isActive"], img_rows)
body += "\n" + "\n".join(
    f"INSERT INTO PropertyAmenity (propertyId, amenityId, active) SELECT {pid}, amenityId, b'1' FROM Amenity WHERE amenityType = 'PROPERTY' AND amenityName IN ({', '.join(q(a) for a in names)});"
    for pid, names in amenity_sets.items())
changeset("0102-demo-properties", "8 approved properties across Vietnam + 1 pending submission, with details, policies, photos and amenities", body,
          "DELETE FROM PropertyAmenity WHERE propertyId BETWEEN 1 AND 9; DELETE FROM propertyimage WHERE propertyId BETWEEN 1 AND 9; DELETE FROM property_policies WHERE propertyId BETWEEN 1 AND 9; DELETE FROM property_details WHERE propertyId BETWEEN 1 AND 9; DELETE FROM properties WHERE propertyId BETWEEN 1 AND 9;")

# rooms
body = insert("rooms", ["roomId", "property_id", "roomName", "roomCategory", "capacity", "area", "pricePerNight", "weekendPrice", "roomAmount", "roomStatus", "isActive", "description", "createdDate"],
              [[str(r[0]), str(r[1]), q(r[2]), q(r[3]), str(r[4]), str(r[5]), str(r[6]), str(r[7]), str(r[8]), q("AVAILABLE"), "b'1'",
                q(f"{r[2]} rộng {r[5]}m², tối đa {r[4]} khách."), "CURDATE()"] for r in rooms])
body += "\n" + insert("roomimage", ["roomImageId", "roomId", "imageUrl", "isCover", "isActive"],
                      [[str(r[0]), str(r[0]), q(IMG.format(r[9])), "b'1'", "b'1'"] for r in rooms])
body += "\n" + "\n".join(
    f"INSERT INTO RoomAmenity (roomId, amenityId, active) SELECT {r[0]}, amenityId, b'1' FROM Amenity WHERE amenityType = 'ROOM' AND amenityName IN ({', '.join(q(a) for a in r[10])});"
    for r in rooms)
changeset("0103-demo-rooms", "15 room types (WHOLE rooms for villas/homestays) with photos and amenities", body,
          "DELETE FROM RoomAmenity WHERE roomId BETWEEN 1 AND 15; DELETE FROM roomimage WHERE roomId BETWEEN 1 AND 15; DELETE FROM rooms WHERE roomId BETWEEN 1 AND 15;")

# promotions
promos = [  # id, code, property, type, value, max, min, rank, start, end, limit, used, banner, desc
    (1, "WELCOME10", None, "PERCENTAGE", 10, 200000, 500000, None, "2026-01-01", "2027-06-30", 1000, 37, "photo-1501117716987-c8c394bb29df", "Giảm 10% cho mọi đơn đặt phòng trên TravelMate"),
    (2, "GOLD15", None, "PERCENTAGE", 15, 500000, 1000000, "GOLD", "2026-06-01", "2027-03-31", 300, 12, "photo-1535827841776-24afc1e255ac", "Ưu đãi 15% dành riêng cho hội viên hạng Vàng trở lên"),
    (3, "FLASH200K", None, "FIXED_AMOUNT", 200000, None, 2000000, None, "2026-10-01", "2026-12-31", 200, 58, "photo-1517840901100-8179e982acb7", "Flash sale: giảm ngay 200.000₫ cho đơn từ 2 triệu"),
    (4, "HANOI50K", 1, "FIXED_AMOUNT", 50000, None, 600000, None, "2026-09-01", "2027-01-31", 100, 9, "photo-1542314831-068cd1dbfeeb", "Giảm 50.000₫ khi lưu trú tại Hanoi Old Quarter Boutique Hotel"),
    (5, "DANANG20", 6, "PERCENTAGE", 20, 800000, 3000000, None, "2026-10-01", "2027-02-28", 80, 5, "photo-1520250497591-112f2f40a3f4", "Mùa biển Đà Nẵng: giảm 20% tại Da Nang Beachfront Resort"),
    (6, "SUMMER2026", None, "PERCENTAGE", 12, 300000, 800000, None, "2026-05-01", "2026-08-31", 500, 214, "photo-1540206351-d6465b3ac5c1", "Khuyến mãi hè 2026 (đã kết thúc)"),
]
body = insert("promotions", ["promotionId", "code", "propertyId", "discountType", "discountValue", "maxDiscountAmount", "minBookingAmount", "minMembershipRank", "startDate", "endDate",
                             "usageLimit", "usageCount", "status", "bannerUrl", "description", "createdAt", "updatedAt"],
              [[str(p[0]), q(p[1]), q(p[2]), q(p[3]), str(p[4]), q(p[5]), str(p[6]), q(p[7]), f"'{p[8]} 00:00:00'", f"'{p[9]} 23:59:59'", str(p[10]), str(p[11]),
                q("EXPIRED" if p[9] < "2026-10-11" else "ACTIVE"), q(IMG.format(p[12])), q(p[13]), "NOW(6)", "NOW(6)"] for p in promos])
changeset("0104-demo-promotions", "Platform (admin) and hotel (owner) promo codes, incl. one rank-gated and one expired", body,
          "DELETE FROM promotions WHERE promotionId BETWEEN 1 AND 6;")

# bookings + payments + refund + ratings + notifications
brow, prow = [], []
for bid, uid, room, cin, cout, guests, status, pstatus, method in bookings:
    u = users_by_id[uid]; total = price(room, cin, cout); pid = ROOM[room][1]
    created = cin - timedelta(days=21)
    refund = (total * Decimal("0.7")).quantize(Decimal("1")) if status == "CANCELLED" else Decimal(0)
    penalty = total - refund if status == "CANCELLED" else Decimal(0)
    brow.append([str(bid), q(uid), str(pid), str(room), q(cin), q(cout), str(guests), str(total), "0", str(penalty), str(refund), q(status),
                 q(u[3]), q(u[4]), q(u[1]), "NULL", f"'{created} 10:00:00'", f"'{created} 10:05:00'"])
    prow.append([str(bid), str(bid), str(total), q(method), q(pstatus), q(f"TRX_DEMO{bid:04d}"), f"'{created} 10:04:00'", f"'{created} 10:04:00'",
                 str(refund) if pstatus == "REFUNDED" else "NULL", f"'{created} 10:00:00'", f"'{created} 10:05:00'"])
body = insert("bookings", ["bookingId", "userId", "propertyId", "roomId", "checkInDate", "checkOutDate", "guestCount", "totalPrice", "discountAmount", "penaltyAmount", "refundAmount",
                           "status", "customerName", "customerPhone", "customerEmail", "specialRequest", "createdAt", "updatedAt"], brow)
body += "\n" + insert("payment", ["paymentId", "bookingId", "totalAmount", "paymentMethod", "paymentStatus", "transactionReference", "paymentDate", "confirmedDate", "refundedAmount", "createdAt", "updatedAt"], prow)
b3 = [b for b in bookings if b[0] == 3][0]
body += "\n" + insert("refund_requests", ["id", "bookingId", "amount", "bankName", "accountNumber", "accountHolder", "reason", "status", "requestDate"], [[
    "1", "3", str((price(b3[2], b3[3], b3[4]) * Decimal("0.7")).quantize(Decimal("1"))), q("Vietcombank"), q("0011001234567"), q("LE HOANG NAM"),
    q("Công ty đột xuất thay đổi lịch công tác nên không thể đi được."), q("PENDING"), "'2026-10-09 09:30:00'"]])
body += "\n" + insert("rating", ["ratingId", "bookings", "users", "rating", "comment", "ratingType", "isHidden", "is_pinned", "createdAt"],
                      [[str(r[0]), str(r[1]), q([b for b in bookings if b[0] == r[1]][0][1]), str(r[2]), q(r[3]), "0", "b'0'", "b'1'" if r[4] else "b'0'",
                        f"'{[b for b in bookings if b[0] == r[1]][0][4]} 15:00:00'"] for r in ratings])
notes = [
    (U(4), "Đặt phòng thành công", "Đơn #2 tại Da Nang Beachfront Resort đã được xác nhận.", "BOOKING_SUCCESS", "2", False),
    (U(4), "Đã hủy đặt phòng", "Đơn #3 đã hủy. Số tiền hoàn dự kiến đang chờ xử lý.", "BOOKING_CANCELLED", "3", False),
    (U(4), "Ưu đãi mới", "Dùng mã FLASH200K để giảm 200.000₫ cho đơn từ 2 triệu!", "PROMOTION", None, True),
    (U(5), "Nâng hạng thành viên", "Chúc mừng! Bạn đã đạt hạng Kim Cương.", "ACCOUNT_UPDATE", None, True),
    (U(2), "Đơn đặt phòng mới #1", "Khách hàng Lê Hoàng Nam vừa đặt phòng tại Hanoi Old Quarter Boutique Hotel.", "BOOKING_RECEIVED", "1", True),
    (U(1), "Yêu cầu hoàn tiền mới", "Đơn phòng #3 đã hủy và đang chờ duyệt hoàn tiền.", "ADMIN_NEW_REFUND_REQUEST", "3", False),
    (U(1), "Có đơn đăng ký Owner mới", "Hoàng Đức Anh vừa gửi đơn đăng ký trở thành chủ nhà.", "ADMIN_NEW_OWNER_REGISTRATION", "3", False),
    (U(1), "Có chỗ nghỉ mới chờ duyệt", "Mui Ne Ocean Villa đang chờ duyệt.", "ADMIN_NEW_PROPERTY_SUBMISSION", "9", False),
]
body += "\n" + insert("notifications", ["user_id", "title", "message", "type", "relatedEntityId", "is_read", "createdAt"],
                      [[q(n[0]), q(n[1]), q(n[2]), q(n[3]), q(n[4]), "b'1'" if n[5] else "b'0'", "NOW(6)"] for n in notes])
changeset("0105-demo-bookings", "Bookings in every state (completed, upcoming, cancelled with refund request), payments, reviews and notifications", body,
          "DELETE FROM notifications WHERE user_id LIKE '00000000-0000-4000-8000-%'; DELETE FROM rating WHERE ratingId BETWEEN 1 AND 5; DELETE FROM refund_requests WHERE id = 1; DELETE FROM payment WHERE paymentId BETWEEN 1 AND 8; DELETE FROM bookings WHERE bookingId BETWEEN 1 AND 8;")

# Seeded notifications as i18n keys (0003 added titleKey/messageKey/params): rendered in each reader's language.
# A separate changeset so 0105 keeps its checksum on databases that already ran it.
refund3 = int((price(b3[2], b3[3], b3[4]) * Decimal("0.7")).quantize(Decimal("1")))
note_keys = [  # (user, seeded title, key, args)
    (U(4), "Đặt phòng thành công", "notification.booking.confirmed", [2, "Da Nang Beachfront Resort"]),
    (U(4), "Đã hủy đặt phòng", "notification.booking.cancelledRefundPending", [3]),
    (U(4), "Ưu đãi mới", "notification.promotion.new", ["FLASH200K", 200000, 2000000]),
    (U(5), "Nâng hạng thành viên", "notification.account.rankUpDiamond", []),
    (U(2), "Đơn đặt phòng mới #1", "notification.booking.received", [1, "Lê Hoàng Nam", "Hanoi Old Quarter Boutique Hotel"]),
    (U(1), "Yêu cầu hoàn tiền mới", "notification.admin.refundRequested", [3, refund3]),
    (U(1), "Có đơn đăng ký Owner mới", "notification.admin.ownerApplication", ["Hoàng Đức Anh"]),
    (U(1), "Có chỗ nghỉ mới chờ duyệt", "notification.admin.propertySubmitted", ["Trần Thị Lan", "Mui Ne Ocean Villa"]),
]
body = "\n".join(
    f"UPDATE notifications SET titleKey = {q(k + '.title')}, messageKey = {q(k + '.message')}, params = {q(json.dumps(a, ensure_ascii=False)) if a else 'NULL'} "
    f"WHERE user_id = {q(u)} AND title = {q(t)} AND titleKey IS NULL;" for u, t, k, a in note_keys)
changeset("0106-demo-notification-keys", "Seeded notifications use i18n keys so they follow the reader's language", body,
          "UPDATE notifications SET titleKey = NULL, messageKey = NULL, params = NULL WHERE user_id LIKE '00000000-0000-4000-8000-%';")

open(OUT, "w", encoding="utf-8").write("\n".join(S))
print("points:", {users_by_id[k][1]: (v, rank(v)) for k, v in points.items()})
print("ratings per property:", prop_stats)
print("booking totals:", {b[0]: int(price(b[2], b[3], b[4])) for b in bookings})
